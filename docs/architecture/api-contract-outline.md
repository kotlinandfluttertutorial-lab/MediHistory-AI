# API Contract Outline — `/api/v1`
- `POST /documents`: create metadata/upload original.
- `GET /documents`: list current user's documents.
- `GET /documents/{id}`: metadata, ownership checked.
- `GET /documents/{id}/content`: authorized content retrieval.
- `POST /documents/{id}/extract`: request OCR.
- `GET /documents/{id}/extraction`: fields, confidence, sources, review state.
- `PUT /documents/{id}/extraction`: save patient corrections.
- `GET /medications`: current/past/unknown entries.
- `POST /duplicate-checks`: compare selected reviewed entries.
- `POST /reports`: create report draft from selected data.
- `GET /reports/{id}`: retrieve report.
- `DELETE /documents/{id}`: request deletion under documented retention semantics.
- `GET /sync/status`: backup status and last successful sync.
All endpoints need validation and authorization. Never put prescription text or medicine names in URLs. Review contracts before implementation.
