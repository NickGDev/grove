package ie.grove.shared;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Discriminator-based multi-tenancy: Hibernate fills {@code creche_id} on insert
 * from the resolver and applies its implicit {@code _tenantId} filter to every
 * query on {@link TenantEntity} subclasses. Spring Boot 4 does not register a
 * {@link CurrentTenantIdentifierResolver} bean automatically, so the customizer
 * below puts it into the Hibernate properties.
 *
 * Hibernate 7 refuses to open a session with a null tenant, so code outside a
 * creche scope (startup, admin seeding) resolves to the synthetic root tenant
 * {@link #ROOT_CRECHE_ID}: {@link #isRoot} disables the filter, so queries see
 * all creches, and any insert left on the root tenant fails the FK loudly
 * instead of bypassing tenancy silently.
 */
@Configuration
class TenancyConfiguration {

    static final Long ROOT_CRECHE_ID = 0L;

    @Bean
    CurrentTenantIdentifierResolver<Long> tenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolver<>() {
            @Override
            public Long resolveCurrentTenantIdentifier() {
                Long crecheId = TenantContext.get();
                return crecheId != null ? crecheId : ROOT_CRECHE_ID;
            }

            @Override
            public boolean isRoot(Long tenantId) {
                return ROOT_CRECHE_ID.equals(tenantId);
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return false;
            }
        };
    }

    @Bean
    HibernatePropertiesCustomizer tenantResolverCustomizer(
            CurrentTenantIdentifierResolver<Long> tenantIdentifierResolver) {
        return properties -> properties.put(
                org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver);
    }
}
