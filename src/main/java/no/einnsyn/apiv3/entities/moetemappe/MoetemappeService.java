package no.einnsyn.apiv3.entities.moetemappe;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MoetemappeService extends MappeService<Moetemappe, MoetemappeDTO> {

  @Getter private final MoetemappeRepository repository;

  @Getter @Lazy @Autowired private MoetemappeService proxy;

  public MoetemappeService(MoetemappeRepository repository) {
    this.repository = repository;
  }

  public Moetemappe newObject() {
    return new Moetemappe();
  }

  public MoetemappeDTO newDTO() {
    return new MoetemappeDTO();
  }

  @Transactional
  public MoetemappeDTO delete(Moetemappe object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }

  // TODO: Implement toDTO, fromDTO

  /**
   * Get Moetedokument list, filtered by Moetemappe
   *
   * @param moetemappeId
   * @param query
   * @return
   */
  public ResultList<MoetedokumentDTO> getMoetedokumentList(
      String moetemappeId, MoetedokumentListQueryDTO query) {
    query.setMoetemappe(moetemappeId);
    var resultPage = moetedokumentService.getPage(query);
    return moetedokumentService.list(query, resultPage);
  }

  public MoetedokumentDTO addMoetedokument(String moetemappeId, MoetedokumentDTO dto) {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetedokumentService.add(dto);
  }

  public MoetemappeDTO removeMoetedokumentFromMoetemappe(
      String moetemappeId, String moetedokumentId) {
    moetedokumentService.delete(moetedokumentId);
    var moetemappe = moetemappeService.findById(moetemappeId);
    return moetemappeService.toDTO(moetemappe);
  }

  /** Moetesak */
  public ResultList<MoetesakDTO> getMoetesakList(String moetemappeId, MoetesakListQueryDTO query) {
    query.setMoetemappe(moetemappeId);
    var resultPage = moetesakService.getPage(query);
    return moetesakService.list(query, resultPage);
  }

  public MoetesakDTO addMoetesak(String moetemappeId, MoetesakDTO dto) {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetesakService.add(dto);
  }

  public MoetemappeDTO removeMoetesakFromMoetemappe(String moetemappeId, String moetesakId) {
    moetesakService.delete(moetesakId);
    var moetemappe = moetemappeService.findById(moetemappeId);
    return moetemappeService.toDTO(moetemappe);
  }
}
