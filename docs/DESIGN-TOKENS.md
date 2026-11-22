# Design tokens and contrast report

Source: `references/design-tokens.json` (v1.0). WCAG 2.2: 4.5:1 normal text, 3:1 large text and UI parts.

| Foreground | Background | Ratio | Result |
|---|---|---|---|
| Deep plum text `#352044` | Background `#FBF7FC` | 13.74 | Pass (text) |
| Secondary text `#61566B` | Background `#FBF7FC` | 6.49 | Pass (text) |
| Secondary text `#61566B` | Surface secondary `#F4EDF8` | 6.00 | Pass (text) |
| White `#FFFFFF` | Primary `#7443B1` | 6.60 | Pass (text) |
| Primary `#7443B1` | Background `#FBF7FC` | 6.23 | Pass (text) |
| Deep plum `#352044` | Primary container `#E7D8FC` | 10.84 | Pass (text) |
| Deep plum `#352044` | Peach `#FFDDD3` | 11.47 | Pass (text) |
| Deep plum `#352044` | Cool mint `#D5EFEB` | 12.05 | Pass (text) |
| Deep plum `#352044` | Powder blue `#DCE8FA` | 11.77 | Pass (text) |
| Secondary text `#61566B` | Peach `#FFDDD3` | 5.42 | Pass (text) |
| Deep teal `#286D65` | Cool mint `#D5EFEB` | 5.01 | Pass (text) |
| Coral (spec) `#C75D58` | Background `#FBF7FC` | 3.86 | Large text / UI only |
| Coral text (adjusted) `#A8443F` | Background `#FBF7FC` | 5.56 | Pass (text) |
| Coral text (adjusted) `#A8443F` | Peach `#FFDDD3` | 4.64 | Pass (text) |
| Error `#B3263E` | Background `#FBF7FC` | 6.07 | Pass (text) |
| Orchid `#C3A6F4` | Background `#FBF7FC` | 1.97 | Decorative only |
| Border `#DDD4E5` | Background `#FBF7FC` | 1.35 | Decorative only |

**Adjustments:** coral used as text darkens to `#A8443F` (`coralText`); orchid and border are decorative only (never carry text or meaning). Wallpaper dynamic color is disabled.

**Prohibited hues:** an automated unit test (`TokenTest`) checks every theme color and fails on hues between 40° and 95° with saturation above 25% (amber, gold, mustard, yellow, lime, chartreuse).
