CREATE TABLE RETRIES_ARCHIVE
(
  RETRY_TYPE          VARCHAR(50)              NOT NULL,
  NATURAL_IDENTIFIER  VARCHAR(513)              NOT NULL,
  RETRY_VER           INTEGER                   NOT NULL,
  PAYLOAD_DATA        BLOB                      NOT NULL,
  CREATE_TS           TIMESTAMP              DEFAULT CURRENT_TIMESTAMP          NOT NULL,
  LAST_UPDATE_TS      TIMESTAMP,
  ORIGINAL_TS TIMESTAMP NOT NULL,
  CONSTRAINT RETRIES_ARCHIVE_TYPE_CK CHECK (RETRY_TYPE = upper(RETRY_TYPE))
)