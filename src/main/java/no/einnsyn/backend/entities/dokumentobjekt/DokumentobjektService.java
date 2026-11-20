package no.einnsyn.backend.entities.dokumentobjekt;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.responses.models.DownloadFileResponse;
import no.einnsyn.backend.common.responses.models.DownloadRedirectResponse;
import no.einnsyn.backend.common.responses.models.DownloadResponseBase;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.backend.entities.downloadcount.DownloadCountService;
import no.einnsyn.backend.utils.SlugGenerator;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class DokumentobjektService extends ArkivBaseService<Dokumentobjekt, DokumentobjektDTO> {
  private static final String DEFAULT_DOWNLOAD_FILE_NAME = "einnsyn-download";
  private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 10_000;
  private static final Duration DOWNLOAD_READ_TIMEOUT = Duration.ofSeconds(30);

  // Some sources allowlist this value, it is inherited from the legacy eInnsyn API.
  private static final String DOWNLOAD_USER_AGENT = "eInnsyn";

  @Getter(onMethod_ = @Override)
  private final DokumentobjektRepository repository;

  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final DownloadCountService downloadCountService;

  @Value("${application.dokumentobjekt.download.proxy.host}")
  private String downloadProxyHost;

  @Value("${application.dokumentobjekt.download.proxy.port:3128}")
  private int downloadProxyPort;

  // URLs that should always work through the proxy. When they don't, the failure is on our side.
  @Value("${application.linkCheck.referenceUrls:}")
  private List<String> referenceUrls;

  // Can be set shorter than for downloads, so sources that don't respond don't hold up the job
  @Value("${application.linkCheck.readTimeout:PT30S}")
  private Duration linkCheckReadTimeout;

  // Assumed to work until a link fails without a response, see isLinkFailure()
  private volatile boolean proxyWorking = true;

  @URL
  @Value("${application.baseUrl}")
  private String baseUrl;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private DokumentobjektService proxy;

  public DokumentobjektService(
      DokumentobjektRepository dokumentobjektRepository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DownloadCountService downloadCountService) {
    this.repository = dokumentobjektRepository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.downloadCountService = downloadCountService;
  }

  @Override
  public Dokumentobjekt newObject() {
    return new Dokumentobjekt();
  }

  @Override
  public DokumentobjektDTO newDTO() {
    return new DokumentobjektDTO();
  }

  /**
   * Override the scheduleIndex method to reindex the parent Dokumentbeskrivelse.
   *
   * @param dokumentobjektId the ID of the dokumentobjekt
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String dokumentobjektId, int recurseDirection) {
    var wasAlreadyScheduled = super.scheduleIndex(dokumentobjektId, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0 && !wasAlreadyScheduled) {
      var dokumentbeskrivelseId =
          dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId);
      if (dokumentbeskrivelseId != null) {
        dokumentbeskrivelseService.scheduleIndex(dokumentbeskrivelseId, -1);
      }
    }

    return wasAlreadyScheduled;
  }

  /**
   * Convert a DTO object to a Dokumentobjekt
   *
   * @param dto The DTO object
   * @param dokumentobjekt The entity object
   * @return The entity object
   */
  @Override
  protected Dokumentobjekt fromDTO(DokumentobjektDTO dto, Dokumentobjekt dokumentobjekt)
      throws EInnsynException {
    super.fromDTO(dto, dokumentobjekt);

    if (dto.getSystemId() != null) {
      dokumentobjekt.setSystemId(dto.getSystemId());
    }

    if (dto.getReferanseDokumentfil() != null) {
      // The link status belongs to the old URL, so a new URL is checked from scratch
      if (dokumentobjekt.getReferanseDokumentfil() != null
          && !dto.getReferanseDokumentfil().equals(dokumentobjekt.getReferanseDokumentfil())) {
        repository.resetLinkStatus(dokumentobjekt.getId());
      }
      dokumentobjekt.setReferanseDokumentfil(dto.getReferanseDokumentfil());
    }

    if (dto.getFormat() != null) {
      dokumentobjekt.setDokumentFormat(dto.getFormat());
    }

    if (dto.getSjekksum() != null) {
      dokumentobjekt.setSjekksum(dto.getSjekksum());
    }

    if (dto.getSjekksumAlgoritme() != null) {
      dokumentobjekt.setSjekksumalgoritme(dto.getSjekksumAlgoritme());
    }

    if (dto.getDokumentbeskrivelse() != null) {
      var dokumentbeskrivelse =
          dokumentbeskrivelseService.findForUpdateOrThrow(dto.getDokumentbeskrivelse());
      dokumentbeskrivelse.addDokumentobjekt(dokumentobjekt);
    }

    return dokumentobjekt;
  }

  /**
   * Convert a Dokumentobjekt to a DTO object
   *
   * @param dokumentobjekt The entity object
   * @param dto The DTO object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The DTO object
   */
  @Override
  protected DokumentobjektDTO toDTO(
      Dokumentobjekt dokumentobjekt,
      DokumentobjektDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(dokumentobjekt, dto, expandPaths, currentPath);

    dto.setSystemId(dokumentobjekt.getSystemId());
    dto.setFormat(dokumentobjekt.getDokumentFormat());
    dto.setSjekksum(dokumentobjekt.getSjekksum());
    dto.setSjekksumAlgoritme(dokumentobjekt.getSjekksumalgoritme());

    // Only expose the download URL when the proxy is configured.
    if (isDownloadProxyConfigured()) {
      var downloadUri =
          UriComponentsBuilder.fromUriString(baseUrl)
              .pathSegment("dokumentobjekt", dokumentobjekt.getId(), "download")
              .build()
              .toUriString();
      dto.setUrl(downloadUri);
    } else {
      dto.setUrl(null);
    }

    // Don't expose source URLs
    if (getProxy().isOwnerOf(dokumentobjekt)) {
      dto.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    }

    var dokumentbeskrivelse = dokumentobjekt.getDokumentbeskrivelse();
    if (dokumentbeskrivelse != null) {
      dto.setDokumentbeskrivelse(
          dokumentbeskrivelseService.maybeExpand(
              dokumentbeskrivelse, "dokumentbeskrivelse", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  public BaseES toLegacyES(Dokumentobjekt dokumentobjekt, BaseES es) {
    super.toLegacyES(dokumentobjekt, es);
    if (es instanceof DokumentobjektES dokumentobjektES) {
      dokumentobjektES.setFormat(dokumentobjekt.getDokumentFormat());
      dokumentobjektES.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    }
    return es;
  }

  @Override
  protected void deleteEntity(Dokumentobjekt dokobj) throws EInnsynException {
    // Download statistics are deliberately kept: the downloads happened, and the buckets carry
    // their own attribution. See DownloadCountService.

    // Remove association to Dokumentbeskrivelse
    if (dokobj.getDokumentbeskrivelse() != null) {
      dokobj.getDokumentbeskrivelse().removeDokumentobjekt(dokobj);
    }

    super.deleteEntity(dokobj);
  }

  /**
   * Download a file for a Dokumentobjekt.
   *
   * <p>HTML references are returned as redirects instead of proxied streams.
   *
   * <p>Not transactional, so no pooled connection is held while waiting for the source.
   *
   * @param id dokumentobjekt id
   * @return download metadata and streaming body
   * @throws EInnsynException if source URL is invalid or unreachable
   */
  public DownloadResponseBase download(String id) throws EInnsynException {
    var sourceUrl = getProxy().getSourceUrl(id);
    var sourceUri = toSourceUri(id, sourceUrl);
    var fileName = getProxy().generateDownloadFileName(id, sourceUri);
    HttpURLConnection connection = null;
    DownloadResponseBase downloadResponse;

    try {
      connection = openDownloadConnection(sourceUri, DOWNLOAD_READ_TIMEOUT);
      var contentType = resolveContentType(connection.getContentType(), sourceUri);

      // Redirect HTML content
      if (isHtmlContentType(contentType)) {
        connection.disconnect();
        var response = new DownloadRedirectResponse();
        response.setLocation(sourceUri.toString());
        downloadResponse = response;
      } else {
        var response = new DownloadFileResponse();
        response.setContentType(contentType);
        response.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        response.setBody(new InputStreamResource(connection.getInputStream()));
        downloadResponse = response;
      }
    } catch (Exception e) {
      if (connection != null) {
        connection.disconnect();
      }
      // Only failures that are certain are recorded. A failure without a response is left for
      // the link check, so the client doesn't wait for a check of the reference URLs.
      if (!isHttpsOnDefaultPort(sourceUri) || isSourceFailure(e)) {
        recordLinkStatus(id, sourceUrl, false);
      }
      // Don't expose source URL in the message, as it will be sent to the client.
      throw new NetworkException("Could not prepare download from source", e, null);
    }

    // Recorded outside the try block: the download has already succeeded at this point, and a
    // failure to record statistics must not be reported to the client as a download failure.
    recordDownload(id);
    recordLinkStatus(id, sourceUrl, isHttpsOnDefaultPort(sourceUri));

    return downloadResponse;
  }

  /**
   * Check whether the source of a Dokumentobjekt responds successfully, without downloading the
   * file, and record the result.
   *
   * <p>Links that aren't HTTPS on port 443 are marked as failed without a request. Other failures
   * are only marked when they are on the source's side (see {@link #isLinkFailure}). On 429 Too
   * Many Requests, only the check timestamp is updated, so a rate limited source isn't asked again
   * until the next round. Failures on our side, and links checked while the proxy is down, are left
   * as they are, so they are checked first when the proxy works again.
   *
   * @param id dokumentobjekt id
   */
  @Transactional(propagation = Propagation.NEVER)
  public void checkLink(String id) {
    if (!isProxyWorking()) {
      return;
    }
    var checkedAt = Instant.now();
    // Deleted, or the link was removed since the batch was fetched
    var sourceUrl =
        repository.findById(id).map(Dokumentobjekt::getReferanseDokumentfil).orElse(null);
    if (sourceUrl == null) {
      return;
    }
    URI sourceUri;
    try {
      sourceUri = toSourceUri(id, sourceUrl);
    } catch (NotFoundException _) {
      // An invalid URL doesn't work
      repository.setLinkFailed(id, sourceUrl, checkedAt);
      return;
    }
    if (!isHttpsOnDefaultPort(sourceUri)) {
      repository.setLinkFailed(id, sourceUrl, checkedAt);
      return;
    }

    HttpURLConnection connection = null;
    try {
      connection = openDownloadConnection(sourceUri, linkCheckReadTimeout);
      repository.setLinkOk(id, sourceUrl, checkedAt);
    } catch (Exception e) {
      log.debug("Link check failed for Dokumentobjekt {}: {}", id, e.getMessage());
      if (isRateLimited(e)) {
        repository.setLinkChecked(id, sourceUrl, checkedAt);
      } else if (isLinkFailure(e)) {
        repository.setLinkFailed(id, sourceUrl, checkedAt);
      }
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /** Whether a download proxy is configured. Without it, files can't be downloaded or checked. */
  public boolean isDownloadProxyConfigured() {
    return StringUtils.hasText(downloadProxyHost);
  }

  /**
   * Check whether any of the reference URLs can be reached through the proxy. When they can't, the
   * proxy is considered down, and the link check is paused until they can be reached again. Without
   * reference URLs, a failure without a response is always assumed to be on our side.
   */
  public void checkReferenceUrls() {
    proxyWorking = referenceUrls.stream().anyMatch(this::isReachable);
  }

  /** Whether the proxy works, according to the latest {@link #checkReferenceUrls}. */
  public boolean isProxyWorking() {
    return proxyWorking;
  }

  /**
   * Whether a failed request means that the link doesn't work. A response from the source proves
   * that the proxy works, except 429, which only means that the source is rate limited. Without a
   * response, the reference URLs are checked, to tell whether the failure is on our side.
   */
  private boolean isLinkFailure(Exception e) {
    if (e instanceof SourceStatusException) {
      return isSourceFailure(e);
    }
    if (!isProxyWorking()) {
      return false;
    }
    checkReferenceUrls();
    return isProxyWorking();
  }

  /** Whether the source responded with an error, other than 429 Too Many Requests. */
  private static boolean isSourceFailure(Exception e) {
    return e instanceof SourceStatusException && !isRateLimited(e);
  }

  private static boolean isRateLimited(Exception e) {
    return e instanceof SourceStatusException sourceStatus
        && sourceStatus.statusCode == HttpStatus.TOO_MANY_REQUESTS.value();
  }

  private boolean isReachable(String url) {
    try {
      openDownloadConnection(URI.create(url), linkCheckReadTimeout).disconnect();
      return true;
    } catch (Exception e) {
      log.debug("Reference URL {} could not be reached: {}", url, e.getMessage());
      return false;
    }
  }

  /**
   * Record whether the link of a Dokumentobjekt works, suppressing any failure. Like download
   * statistics, this is a best-effort side effect that must not change the response to the client.
   *
   * @param id dokumentobjekt id
   * @param sourceUrl the URL that was downloaded, as stored
   * @param ok whether the link works
   */
  private void recordLinkStatus(String id, String sourceUrl, boolean ok) {
    try {
      if (ok) {
        repository.setLinkOk(id, sourceUrl, Instant.now());
      } else {
        repository.setLinkFailed(id, sourceUrl, Instant.now());
      }
    } catch (Exception e) {
      log.error("Could not record link status for Dokumentobjekt {}", id, e);
    }
  }

  /**
   * Record a download for statistics, suppressing any failure.
   *
   * <p>Download statistics are a best-effort side effect of a download that has already succeeded.
   * Letting an exception escape here would turn a working download into an error response, so
   * failures are logged and swallowed.
   *
   * @param id dokumentobjekt id
   */
  private void recordDownload(String id) {
    try {
      downloadCountService.recordDownload(id);
    } catch (Exception e) {
      log.error("Could not record download statistics for Dokumentobjekt {}", id, e);
    }
  }

  /**
   * Look up the source URL for a Dokumentobjekt, as stored.
   *
   * @param id the dokumentobjekt id
   * @return the source URL
   * @throws EInnsynException if the object is not found or has no source URL
   */
  @Transactional(readOnly = true)
  public String getSourceUrl(String id) throws EInnsynException {
    authorizeGet(id);
    var dokumentobjekt = getProxy().findOrThrow(id, NotFoundException.class);
    var sourceUrl = dokumentobjekt.getReferanseDokumentfil();
    if (sourceUrl == null || sourceUrl.isBlank()) {
      throw new NotFoundException("No source found for " + id);
    }
    return sourceUrl;
  }

  /**
   * Convert a stored source URL to a URI.
   *
   * @param id the dokumentobjekt id, for the error message
   * @param sourceUrl the source URL, as stored
   * @return the source URI
   * @throws NotFoundException if the source URL is invalid
   */
  private static URI toSourceUri(String id, String sourceUrl) throws NotFoundException {
    try {
      // Preserve already-encoded source URLs exactly as stored.
      return URI.create(sourceUrl);
    } catch (IllegalArgumentException _) {
      // Fall back to encoding for user-provided URLs with spaces/non-ASCII.
      try {
        return UriComponentsBuilder.fromUriString(sourceUrl).encode().build().toUri();
      } catch (Exception e) {
        throw new NotFoundException("Invalid source URL for " + id, e);
      }
    }
  }

  /**
   * Generate a human-readable download file name for a Dokumentobjekt. Uses the source URI file
   * name when available, falling back to the Dokumentbeskrivelse title and document number.
   *
   * @param id the dokumentobjekt id
   * @param sourceUri the source URI to extract a fallback file name from
   * @return the generated file name
   * @throws NotFoundException if the dokumentobjekt is not found
   */
  @Transactional(readOnly = true)
  public String generateDownloadFileName(String id, URI sourceUri) throws NotFoundException {
    var dokumentobjekt = getProxy().findOrThrow(id, NotFoundException.class);
    var sourceFilename = StringUtils.getFilename(sourceUri.getPath());
    var rawExtension = StringUtils.getFilenameExtension(sourceFilename);
    var extension = SlugGenerator.generate(rawExtension);
    var baseFileName = "";

    // If the source has a filename, use it directly, but sanitize it
    if (StringUtils.hasText(sourceFilename)) {
      var basePart = StringUtils.stripFilenameExtension(sourceFilename);
      baseFileName = SlugGenerator.generate(basePart);
    }

    // Try to get filename from the Dokumentbeskrivelse title and document number
    if (!StringUtils.hasText(baseFileName)) {
      var dokumentbeskrivelse = dokumentobjekt.getDokumentbeskrivelse();
      if (dokumentbeskrivelse != null) {
        var title = SlugGenerator.generate(dokumentbeskrivelse.getTittel());
        var dokumentnummer = dokumentbeskrivelse.getDokumentnummer();
        if (StringUtils.hasText(title) && dokumentnummer != null) {
          baseFileName = title + "-" + dokumentnummer;
        } else if (StringUtils.hasText(title)) {
          baseFileName = title;
        } else if (dokumentnummer != null) {
          baseFileName = DEFAULT_DOWNLOAD_FILE_NAME + "-" + dokumentnummer;
        }
      }
    }

    // No useful info in the Dokumentbeskrivelse
    if (!StringUtils.hasText(baseFileName)) {
      baseFileName = DEFAULT_DOWNLOAD_FILE_NAME + "-" + dokumentobjekt.getId();
    }

    // If source filename didn't have an extension, get the extension from the dokument format
    if (!StringUtils.hasText(extension)
        && StringUtils.hasText(dokumentobjekt.getDokumentFormat())) {
      extension = SlugGenerator.generate(dokumentobjekt.getDokumentFormat());
    }

    // Return baseFileName with extension if available, otherwise just baseFileName
    return StringUtils.hasText(extension) ? baseFileName + "." + extension : baseFileName;
  }

  /**
   * Open a connection to the source URI through the configured download proxy. SSRF protection and
   * scheme enforcement are delegated to the proxy.
   */
  private HttpURLConnection openDownloadConnection(URI sourceUri, Duration readTimeout)
      throws IOException {
    var proxyAddress = new InetSocketAddress(downloadProxyHost, downloadProxyPort);
    var httpProxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
    var connection = (HttpURLConnection) sourceUri.toURL().openConnection(httpProxy);
    connection.setRequestProperty(HttpHeaders.USER_AGENT, DOWNLOAD_USER_AGENT);
    connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT_MS);
    connection.setReadTimeout((int) readTimeout.toMillis());
    connection.setInstanceFollowRedirects(true);
    connection.connect();

    var statusCode = connection.getResponseCode();
    if (statusCode < 200 || statusCode >= 300) {
      var message = connection.getResponseMessage();
      connection.disconnect();
      throw new SourceStatusException(
          statusCode,
          "Source returned HTTP "
              + statusCode
              + (StringUtils.hasText(message) ? " (" + message + ")" : "")
              + " for "
              + sourceUri);
    }

    return connection;
  }

  /**
   * Fulltext files must be served over HTTPS on the default port, since the proxy only allows port
   * 443. A link with any other scheme or port doesn't work.
   */
  private boolean isHttpsOnDefaultPort(URI sourceUri) {
    var port = sourceUri.getPort();
    return "https".equalsIgnoreCase(sourceUri.getScheme()) && (port == -1 || port == 443);
  }

  /** Check whether the content type indicates HTML or XHTML content. */
  private boolean isHtmlContentType(String contentType) {
    if (StringUtils.hasText(contentType)) {
      var normalized = contentType.toLowerCase(Locale.ROOT);
      return normalized.startsWith("text/html") || normalized.startsWith("application/xhtml+xml");
    }
    return false;
  }

  /**
   * Resolve the content type for a download response. Uses the server-provided content type if
   * available, otherwise guesses from the URI path, defaulting to {@code application/octet-stream}.
   */
  private String resolveContentType(String contentType, URI referanseUri) {
    if (StringUtils.hasText(contentType)) {
      return contentType;
    }

    var guessed = URLConnection.guessContentTypeFromName(referanseUri.getPath());
    if (guessed != null && !guessed.isBlank()) {
      return guessed;
    }
    return "application/octet-stream";
  }

  /** Thrown when the source responds with a status code other than 2xx. */
  private static class SourceStatusException extends IOException {
    private final int statusCode;

    SourceStatusException(int statusCode, String message) {
      super(message);
      this.statusCode = statusCode;
    }
  }
}
