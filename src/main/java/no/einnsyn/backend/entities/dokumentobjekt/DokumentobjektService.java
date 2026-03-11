package no.einnsyn.backend.entities.dokumentobjekt;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
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
import no.einnsyn.backend.utils.SlugGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DokumentobjektService extends ArkivBaseService<Dokumentobjekt, DokumentobjektDTO> {
  private static final String DEFAULT_DOWNLOAD_FILE_NAME = "einnsyn-download";
  private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 10_000;
  private static final int DOWNLOAD_READ_TIMEOUT_MS = 30_000;

  @Getter(onMethod_ = @Override)
  private final DokumentobjektRepository repository;

  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;

  @Value("${application.dokumentobjekt.download.proxy.host}")
  private String downloadProxyHost;

  @Value("${application.dokumentobjekt.download.proxy.port:3128}")
  private int downloadProxyPort;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private DokumentobjektService proxy;

  public DokumentobjektService(
      DokumentobjektRepository dokumentobjektRepository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository) {
    this.repository = dokumentobjektRepository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
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
    var isScheduled = super.scheduleIndex(dokumentobjektId, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0 && !isScheduled) {
      var dokumentbeskrivelseId =
          dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId);
      if (dokumentbeskrivelseId != null) {
        dokumentbeskrivelseService.scheduleIndex(dokumentbeskrivelseId, -1);
      }
    }

    return true;
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
          dokumentbeskrivelseService.findByIdOrThrow(dto.getDokumentbeskrivelse().getId());
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
   * @param id dokumentobjekt id
   * @return download metadata and streaming body
   * @throws EInnsynException if source URL is invalid or unreachable
   */
  @Transactional(propagation = Propagation.NEVER)
  public DownloadResponseBase download(String id) throws EInnsynException {
    var sourceUri = getProxy().getSourceUri(id);
    var fileName = getProxy().generateDownloadFileName(id, sourceUri);
    HttpURLConnection connection = null;

    try {
      connection = openDownloadConnection(sourceUri);
      var contentType = resolveContentType(connection.getContentType(), sourceUri);

      // Redirect HTML content
      if (isHtmlContentType(contentType)) {
        connection.disconnect();
        var response = new DownloadRedirectResponse();
        response.setLocation(sourceUri.toString());
        return response;
      }

      var response = new DownloadFileResponse();
      response.setContentType(contentType);
      response.setContentDisposition("attachment; filename=\"" + fileName + "\"");
      response.setBody(new InputStreamResource(connection.getInputStream()));
      return response;
    } catch (Exception e) {
      if (connection != null) {
        connection.disconnect();
      }
      // Don't expose source URL in the message, as it will be sent to the client.
      throw new NetworkException("Could not prepare download from source", e, null);
    }
  }

  /**
   * Look up the source URI for a Dokumentobjekt.
   *
   * @param id the dokumentobjekt id
   * @return the source URI
   * @throws EInnsynException if the object is not found or has no valid source URL
   */
  @Transactional(readOnly = true)
  public URI getSourceUri(String id) throws EInnsynException {
    authorizeGet(id);
    var dokumentobjekt = getProxy().findByIdOrThrow(id, NotFoundException.class);
    var sourceUrl = dokumentobjekt.getReferanseDokumentfil();
    if (sourceUrl == null || sourceUrl.isBlank()) {
      throw new NotFoundException("No source found for " + id);
    }

    try {
      // Preserve already-encoded source URLs exactly as stored.
      return URI.create(sourceUrl);
    } catch (IllegalArgumentException ignored) {
      try {
        return UriComponentsBuilder.fromUriString(sourceUrl).encode().build().toUri();
      } catch (IllegalArgumentException e) {
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
    var dokumentobjekt = getProxy().findByIdOrThrow(id, NotFoundException.class);
    var sourceFilename = StringUtils.getFilename(sourceUri.getPath());
    var rawExtension = StringUtils.getFilenameExtension(sourceFilename);
    var extension = SlugGenerator.generate(rawExtension);
    var baseFileName = "";

    // If the source has a filename, use it directly, but sanitize it
    if (StringUtils.hasText(sourceFilename)) {
      var basePart =
          StringUtils.hasText(extension)
              ? sourceFilename.substring(0, sourceFilename.length() - rawExtension.length() - 1)
              : sourceFilename;
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
  private HttpURLConnection openDownloadConnection(URI sourceUri) throws IOException {
    var proxyAddress = new InetSocketAddress(downloadProxyHost, downloadProxyPort);
    var httpProxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
    var connection = (HttpURLConnection) sourceUri.toURL().openConnection(httpProxy);
    connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS);
    connection.connect();

    var statusCode = connection.getResponseCode();
    if (statusCode < 200 || statusCode >= 300) {
      var message = connection.getResponseMessage();
      connection.disconnect();
      throw new IOException(
          "Source returned HTTP "
              + statusCode
              + (StringUtils.hasText(message) ? " (" + message + ")" : "")
              + " for "
              + sourceUri);
    }

    return connection;
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
}
