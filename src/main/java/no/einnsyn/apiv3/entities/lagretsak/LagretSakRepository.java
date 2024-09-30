package no.einnsyn.apiv3.entities.lagretsak;

import java.util.stream.Stream;
import no.arkivverket.standarder.noark5.arkivmelding.Saksmappe;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSak;

public interface LagretSakRepository extends BaseRepository<LagretSak> {
  Stream<LagretSak> findBySaksmappeAndAbonnereTrueOrderById(Saksmappe saksmappe);
}
