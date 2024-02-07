package no.einnsyn.apiv3.entities.moetemappe;

import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetemappeService extends MappeService<Moetemappe, MoetemappeDTO> {

  @Getter private final MoetemappeRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetemappeService proxy;

  public MoetemappeService(MoetemappeRepository repository) {
    this.repository = repository;
  }

  public Moetemappe newObject() {
    return new Moetemappe();
  }

  public MoetemappeDTO newDTO() {
    return new MoetemappeDTO();
  }

  // TODO: Implement toDTO, fromDTO

  @Transactional
  public MoetemappeDTO delete(Moetemappe object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }

  // Moetedokument
  public ResultList<MoetedokumentDTO> getMoetedokumentList(
      String moetemappeId, MoetedokumentListQueryDTO query) {
    query.setMoetemappeId(moetemappeId);
    return moetedokumentService.list(query);
  }

  public MoetedokumentDTO addMoetedokument(String moetemappeId, MoetedokumentDTO dto)
      throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetedokumentService.add(dto);
  }

  // Moetesak
  public ResultList<MoetesakDTO> getMoetesakList(String moetemappeId, MoetesakListQueryDTO query) {
    query.setMoetemappeId(moetemappeId);
    return moetesakService.list(query);
  }

  public MoetesakDTO addMoetesak(String moetemappeId, MoetesakDTO dto) throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetesakService.add(dto);
  }

  public MoetemappeDTO deleteMoetesak(String moetemappeId, String moetesakId)
      throws EInnsynException {
    moetesakService.delete(moetesakId);
    var moetemappe = moetemappeService.findById(moetemappeId);
    return moetemappeService.toDTO(moetemappe);
  }
}
