CREATE TABLE applications (
                              id BIGSERIAL PRIMARY KEY,

                              application_number VARCHAR(30) NOT NULL,
                              tenant_id VARCHAR(100) NOT NULL,

                              applicant_uuid VARCHAR(100) NOT NULL,
                              applicant_mobile VARCHAR(20) NOT NULL,

                              road_type VARCHAR(20) NOT NULL,
                              length_in_meters NUMERIC(12, 3) NOT NULL,
                              width_in_meters NUMERIC(12, 3) NOT NULL,
                              duration_in_days INTEGER NOT NULL,

                              applicant_type VARCHAR(30) NOT NULL,
                              proposed_start_date DATE NOT NULL,

                              area_in_sqm NUMERIC(12, 2) NOT NULL,
                              restoration_charge NUMERIC(15, 2) NOT NULL,
                              permission_fee NUMERIC(15, 2) NOT NULL,
                              urgency_surcharge NUMERIC(15, 2) NOT NULL,
                              security_deposit NUMERIC(15, 2) NOT NULL,
                              total_amount NUMERIC(15, 2) NOT NULL,

                              status VARCHAR(30) NOT NULL,

                              created_by VARCHAR(100) NOT NULL,
                              created_time TIMESTAMP NOT NULL,
                              last_modified_by VARCHAR(100) NOT NULL,
                              last_modified_time TIMESTAMP NOT NULL,

                              version BIGINT NOT NULL DEFAULT 0,

                              CONSTRAINT uk_application_number
                                  UNIQUE (application_number)
);

CREATE INDEX idx_applications_tenant
    ON applications (tenant_id);

CREATE INDEX idx_applications_tenant_status
    ON applications (tenant_id, status);

CREATE INDEX idx_applications_tenant_mobile
    ON applications (tenant_id, applicant_mobile);

CREATE INDEX idx_applications_tenant_applicant
    ON applications (tenant_id, applicant_uuid);


CREATE TABLE application_transitions (
                                         id BIGSERIAL PRIMARY KEY,

                                         application_id BIGINT NOT NULL,

                                         from_status VARCHAR(30),
                                         action VARCHAR(30) NOT NULL,
                                         to_status VARCHAR(30) NOT NULL,

                                         actor_uuid VARCHAR(100) NOT NULL,
                                         actor_username VARCHAR(100) NOT NULL,
                                         actor_role VARCHAR(30) NOT NULL,

                                         comment VARCHAR(1000),

                                         created_time TIMESTAMP NOT NULL,

                                         CONSTRAINT fk_transition_application
                                             FOREIGN KEY (application_id)
                                                 REFERENCES applications (id)
                                                 ON DELETE CASCADE
);

CREATE INDEX idx_transitions_application
    ON application_transitions (application_id, created_time);


CREATE TABLE application_sequences (
                                       tenant_id VARCHAR(100) NOT NULL,
                                       financial_year VARCHAR(10) NOT NULL,
                                       module_code VARCHAR(20) NOT NULL,
                                       next_value BIGINT NOT NULL,

                                       PRIMARY KEY (tenant_id, financial_year, module_code)
);