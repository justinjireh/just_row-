# Docs Index

This file is the canonical document index for the repository. It points to every current brief and every current LLM handoff prompt in one place.

## Fastest Read Order

If you are new to the project, read in this order:

1. [START_HERE.md](START_HERE.md)
2. [../README.md](../README.md)
3. [../HYDROW_CUSTOM_APP_BRIEF.md](../HYDROW_CUSTOM_APP_BRIEF.md)
4. [../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)
5. [RELEASE_NOTES_v0.1_PROTOTYPE.md](RELEASE_NOTES_v0.1_PROTOTYPE.md)
6. [../device_recon/HYDROW_PACKAGE_RECON.md](../device_recon/HYDROW_PACKAGE_RECON.md)
7. [../NEXT_STEPS.md](../NEXT_STEPS.md)

## Briefs

Primary product and engineering brief:

- [../HYDROW_CUSTOM_APP_BRIEF.md](../HYDROW_CUSTOM_APP_BRIEF.md)

Live implementation and install status:

- [../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)

Prototype release note for the current installed build:

- [RELEASE_NOTES_v0.1_PROTOTYPE.md](RELEASE_NOTES_v0.1_PROTOTYPE.md)

Telemetry reconnaissance summary:

- [../device_recon/HYDROW_PACKAGE_RECON.md](../device_recon/HYDROW_PACKAGE_RECON.md)

## Handoff Prompts

General continuation:

- [../LLM_HANDOFF_PROMPT.md](../LLM_HANDOFF_PROMPT.md)

Coding and implementation:

- [../LLM_HANDOFF_PROMPT_CODING.md](../LLM_HANDOFF_PROMPT_CODING.md)

Reverse engineering and telemetry mapping:

- [../LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md](../LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md)

Product and UX refinement:

- [../LLM_HANDOFF_PROMPT_PRODUCT_UX.md](../LLM_HANDOFF_PROMPT_PRODUCT_UX.md)

## Contributor Docs

Repository workflow and safety rules:

- [../CONTRIBUTING.md](../CONTRIBUTING.md)

Prioritized engineering backlog:

- [../NEXT_STEPS.md](../NEXT_STEPS.md)

## App Source And Build Entry Points

Top-level repository overview:

- [../README.md](../README.md)

Android project build notes:

- [../hydrow-rowplus-launcher/README.md](../hydrow-rowplus-launcher/README.md)

Android project root:

- [../hydrow-rowplus-launcher](../hydrow-rowplus-launcher)

## Reverse Engineering Inputs

Pulled APKs tracked in Git LFS:

- `../device_apks/HydrowLauncher.apk`
- `../device_apks/crew-base.apk`

## Recommended Contributor Flow

1. Read [START_HERE.md](START_HERE.md).
2. Review the two main briefs and the current release note.
3. Pick the correct handoff prompt for the task.
4. Review [../hydrow-rowplus-launcher/README.md](../hydrow-rowplus-launcher/README.md) before changing code.
5. Make one incremental change at a time and include device test and rollback notes.
