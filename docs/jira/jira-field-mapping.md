# Jira CSV Field Mapping
Import `medihistory-jira.csv` using Jira CSV import. Map Issue Type, Summary, Description, Priority, Labels. Map Acceptance Criteria to a custom field if available; otherwise keep it in Description. Component may map to Components. Requirements, Dependencies, Skills, and Kiro Prompt can be custom fields or retained in Description.
If Jira does not accept issue keys, let Jira generate them and update prompt references. Import parent/epic structure according to your Jira configuration. Confirm field names and allowed values before bulk import.
