# Create compatibility

This directory is an additional resource root (`sourceSets.main.resources { srcDir 'compat/create' }`).

- `data/create/recipe/compacting/rubber_from_latex.json`
  - 250 mB `sap_extractor:latex` → 1 `sap_extractor:rubber`
  - guarded by `neoforge:mod_loaded: create`

The extractor itself has no compile-time Create dependency. Container detection uses the
`sap_extractor:containers` block tag (optional `create:basin` entry) plus the NeoForge
fluid handler capability, and the compacting recipe is a plain data file.
