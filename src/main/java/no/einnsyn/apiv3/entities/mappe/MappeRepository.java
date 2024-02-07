package no.einnsyn.apiv3.entities.mappe;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MappeRepository<T extends Mappe> extends ArkivBaseRepository<T> {}
