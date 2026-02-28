# Docs Index

This folder is the clean entry point for contributors and LLM handoff.

It does not duplicate every brief. Instead, it organizes the existing documents and points to the right one for each task.

## Read Order

If you are new to the project, read in this order:

1. [../README.md](../README.md)
2. [../HYDROW_CUSTOM_APP_BRIEF.md](../HYDROW_CUSTOM_APP_BRIEF.md)
3. [../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)
4. [../device_recon/HYDROW_PACKAGE_RECON.md](../device_recon/HYDROW_PACKAGE_RECON.md)
5. [../NEXT_STEPS.md](../NEXT_STEPS.md)

## Choose The Right Handoff Prompt

General continuation:

- [../LLM_HANDOFF_PROMPT.md](../LLM_HANDOFF_PROMPT.md)

Coding / app implementation:

- [../LLM_HANDOFF_PROMPT_CODING.md](../LLM_HANDOFF_PROMPT_CODING.md)

Reverse engineering / telemetry mapping:

- [../LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md](../LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md)

Product / UX refinement:

- [../LLM_HANDOFF_PROMPT_PRODUCT_UX.md](../LLM_HANDOFF_PROMPT_PRODUCT_UX.md)

## Core Project Docs

High-level product and engineering brief:

- [../HYDROW_CUSTOM_APP_BRIEF.md](../HYDROW_CUSTOM_APP_BRIEF.md)

Current implementation and installation status:

- [../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)

Repo collaboration and safety guidance:

- [../CONTRIBUTING.md](../CONTRIBUTING.md)

Prioritized backlog:

- [../NEXT_STEPS.md](../NEXT_STEPS.md)

## Reverse Engineering Inputs

APK reconnaissance note:

- [../device_recon/HYDROW_PACKAGE_RECON.md](../device_recon/HYDROW_PACKAGE_RECON.md)

Pulled APKs tracked in Git LFS:

- `../device_apks/HydrowLauncher.apk`
- `../device_apks/crew-base.apk`

## App Source

Android project root:

- [../hydrow-rowplus-launcher](../hydrow-rowplus-launcher)

## Recommended Contributor Flow

1. Read the brief and implementation status.
2. Pick the correct handoff prompt for your task.
3. Review the current app source.
4. Make an incremental change.
5. Include device test steps and rollback notes.

