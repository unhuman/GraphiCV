# GraphiCV

**Graphics / Sprite Editor for ColecoVision and TI-99/4A**
by unHUman / Howard Uman

GraphiCV is a desktop editor for the 16×16 pixel graphics used by the TMS9918A video
chip found in the ColecoVision, the TI-99/4A, the MSX, and the SG-1000. You draw on a
16×16 grid, pick colors from the chip's 16-color palette, and GraphiCV continuously
emits the hex data you paste straight into your game's source — in ColecoVision C,
TI-99 Extended BASIC, or TI-99 assembler format.

It also works in reverse: paste existing hex data into any of the text fields, press
Enter, and the image appears on the grid.

---

## Table of contents

- [Quick start](#quick-start)
- [Building from source](#building-from-source)
- [Building a native application](#building-a-native-application)
- [Concepts](#concepts)
- [Using the editor](#using-the-editor)
- [Output formats](#output-formats)
- [Importing existing data](#importing-existing-data)
- [Keyboard and mouse reference](#keyboard-and-mouse-reference)
- [Project layout](#project-layout)
- [Known limitations](#known-limitations)

---

## Quick start

If you already have Java 17 or newer installed:

```sh
mvn clean package
java -jar target/GraphiCV-1.0.jar
```

If you'd rather hand someone an application that does not require Java at all, see
[Building a native application](#building-a-native-application).

---

## Building from source

**Requirements**

| | |
|---|---|
| JDK | 17 or newer (25 is what the project is developed against) |
| Maven | 3.6 or newer |

```sh
mvn clean package
```

This produces `target/GraphiCV-1.0.jar`, a runnable jar with the toolbar icons bundled
inside it and `Main-Class` already set in the manifest. Run it with
`java -jar target/GraphiCV-1.0.jar`, or on most desktops by double-clicking the jar.

There is no test suite and there are no third-party dependencies — GraphiCV is pure Java
Swing.

---

## Building a native application

The `packaging/` scripts wrap [`jpackage`](https://docs.oracle.com/en/java/javase/17/jpackage/)
(shipped with every JDK 14+) to produce a **self-contained application with a trimmed
Java runtime baked in**. Users of the result do not need Java installed.

`jpackage` can only build for the platform it runs on: build the Windows app on Windows
and the macOS app on macOS.

### Windows

```powershell
.\packaging\package-windows.ps1
```

Produces `target\dist\GraphiCV\GraphiCV.exe` plus its `app\` and `runtime\` folders
(roughly 120 MB total). The whole `target\dist\GraphiCV` folder is the deliverable — zip
it and it runs anywhere, no install step, no Java.

To build a real installer instead:

```powershell
.\packaging\package-windows.ps1 -Installer            # .exe installer (default)
.\packaging\package-windows.ps1 -Installer -Type msi  # .msi installer
```

Installers add Start Menu and desktop shortcut options and let the user choose an install
directory. Note that **installer creation requires the [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases)
on your PATH**; the plain app-image build above needs nothing but a JDK.

### macOS

```sh
chmod +x packaging/package-mac.sh    # first time only
./packaging/package-mac.sh
```

Produces `target/dist/GraphiCV.app`. To build a disk image or installer package:

```sh
./packaging/package-mac.sh --installer              # .dmg (default)
./packaging/package-mac.sh --installer --type pkg   # .pkg
```

The resulting app is **unsigned**. Gatekeeper will refuse to open it on another Mac
unless the user right-clicks the app and chooses *Open* the first time. To sign during
the build, export a Developer ID identity first:

```sh
export MAC_SIGNING_IDENTITY="Developer ID Application: Your Name (TEAMID)"
./packaging/package-mac.sh --installer
```

Distributing outside the App Store additionally requires notarization
(`xcrun notarytool`), which the script does not do for you.

### Application icons

Both scripts pick up an icon automatically if you drop one in `packaging/`:

- Windows: `packaging/GraphiCV.ico`
- macOS: `packaging/GraphiCV.icns`

Without them, the default Java icon is used. Everything else works unchanged.

---

## Concepts

Understanding three things makes the whole UI obvious.

**1. The grid is four characters, not one image.**
The TMS9918A draws in 8×8 character patterns. GraphiCV's 16×16 canvas is really a 2×2
block of four of those patterns. GraphiCV calls them *quadrants*, and they are numbered
in the column-major order the hardware expects:

```
+-----------+-----------+
|     0     |     2     |
| Top-Left  | Top-Right |
+-----------+-----------+
|     1     |     3     |
|Bottom-Left|Bottom-Rgt |
+-----------+-----------+
```

Each quadrant is 8 bytes (one byte per row), which is why each hex field holds exactly
16 hex characters.

**2. Color is per row, not per pixel.**
The hardware allows one foreground and one background color per 8-pixel row of a
character. GraphiCV mirrors that exactly: the color swatch columns beside the grid give
you a foreground/background pair for every row. You cannot color individual pixels,
because the console cannot either.

**3. There are five layers.**
The *Level* dropdown switches between `Sprite 1`–`Sprite 4` and `Background`. Each is an
independent 16×16 image with its own colors, and the preview strip composites them so you
can see how they stack. Sprites use a single color with transparency; the background
layer is the one that gets full per-row foreground/background coloring.

---

## Using the editor

### Drawing

- **Click** a pixel to toggle it between foreground and background.
- **Click and drag** to paint a run of pixels. The state you set on mouse-down is the
  state that gets painted across the drag.
- **Arrow keys** move focus between pixels — including wrapping across quadrant
  boundaries — and **Space** toggles the focused pixel, so you can draw entirely from the
  keyboard.

### Colors

Left of the grid are two color columns:

- The **master pair** (leftmost, a single foreground/background pair) sets the color for
  *every* row at once. This is the fast way to recolor a whole image.
- The **per-row pairs** set foreground/background for one 8-pixel row of one quadrant.

Clicking any swatch opens the 16-color TMS9918A palette. Index 0 is *transparent*; the
preview background color chooser excludes it, since a backdrop cannot be transparent.

By default the right half of the image reuses the left half's row colors. Tick
**Enable different colors** to give the right-hand characters their own color rows —
this reveals a second set of swatches on the right side of the grid and enables the
`Top-Right` / `Bottom-Right` color map fields. The checkbox is only available while
editing the `Background` level, because sprites are single-colored by hardware.

### Layers and the preview

The preview strip on the right shows all five layers plus the composited result:

- **Left-click** a preview (or its label) to switch the editor to that layer.
- **Right-click** a preview for a menu with **Copy To…** and **Swap With…**, letting you
  duplicate a layer onto another or exchange two layers.
- **Preview BG Color** sets the backdrop the previews composite against — useful for
  checking that a sprite reads well over your actual screen color. It has no effect on
  exported data.

Sprite layers force their background color to transparent, so anything you leave unset
shows the layers beneath it.

### Manipulation tools

| Button | Effect |
|---|---|
| Reset | Prompts to clear pixels on the current layer only, or on all five layers. Colors are not reset — use the master swatches for that. |
| Invert Pixels | Swaps foreground and background pixels; colors stay put. |
| Invert Colors | Swaps the foreground and background *colors* everywhere. Background layer only. |
| Push Left / Right / Up / Down | Shifts the image one pixel, filling the vacated edge with background. Vertical pushes shift the row colors along with the pixels. |
| Mirror | Flips the image horizontally, carrying row colors with it. |
| Flip | Flips the image vertically, carrying row colors with it. |
| Rotate CW / CCW | Rotates 90°. Because per-row colors cannot survive a rotation, this resets colors to the master pair when editing the background layer; GraphiCV warns you once per session before doing it. |

The movement tools use icons from `resource/images/`; Reset, Invert Pixels, and Invert
Colors are text buttons, which also makes their enabled/disabled state easy to read at a
glance — Invert Colors greys out whenever you are not on the `Background` level.

---

## Output formats

The **System** dropdown at the top left changes how every hex field is formatted. The
data is identical; only the syntax differs, so you can paste directly into your source.

The **Graphics** section gives you the four quadrants separately plus a `Full` field with
all four concatenated in quadrant order (Top-Left, Bottom-Left, Top-Right, Bottom-Right).
The **Color Maps** section gives one field per quadrant.

### TI-99 XB (TI-99/4A Extended BASIC)

Plain uppercase hex, 16 characters per quadrant — exactly what `CALL CHAR` wants:

```
00183C7EFF7E3C18
```

### TI-99 Asm (TI-99/4A assembler)

Word-sized `DATA`-style constants, two bytes per `>` group:

```
>0018,>3C7E,>FF7E,>3C18
```

Note that this setting affects the graphics fields only; color maps stay in plain hex
format.

### ColecoVision (C)

A brace-wrapped byte array ready to drop into a C source file:

```
{0x00, 0x18, 0x3C, 0x7E, 0xFF, 0x7E, 0x3C, 0x18}
```

The `Full` field merges all four quadrants into one 32-byte array.

### How the bytes are built

- **Graphics:** each row of 8 pixels becomes one byte, MSB = leftmost pixel, 1 = foreground.
- **Color maps:** each row becomes one byte whose high nibble is the foreground palette
  index and low nibble the background index, matching the VDP color table layout. Palette
  index 0 is transparent.

---

## Importing existing data

Every hex field is editable. Paste data in, press **Enter**, and the grid updates.

The parser is deliberately forgiving — it strips `0x` prefixes, `>` prefixes, braces,
commas, whitespace, and anything before an `=` sign, so you can paste a whole C or
assembler declaration without cleaning it up first. What remains must be a multiple of 16
hex characters, and no more than will fit from the field you pasted into onward. Pasting
32 characters into the `Top-Left` field fills Top-Left *and* Bottom-Left, for example.

A field that turns **yellow** means the input was rejected; it goes back to white on the
next successful parse or recalculation. Pasting color data that reaches into the
right-hand quadrants automatically ticks **Enable different colors** for you.

---

## Keyboard and mouse reference

| Input | Action |
|---|---|
| Click pixel | Toggle foreground/background |
| Click + drag across pixels | Paint the dragged run |
| Arrow keys | Move pixel focus (wraps across quadrants) |
| Space / Enter on a focused pixel | Toggle it |
| Click color swatch | Open the 16-color palette chooser |
| Left-click a preview or its label | Switch the editor to that layer |
| Right-click a preview or its label | Copy To… / Swap With… menu |
| Enter in a hex field | Parse and load that hex into the grid |

---

## Project layout

```
GraphiCV/
├── pom.xml                        Maven build; bundles resource/ into the jar
├── packaging/
│   ├── package-windows.ps1        jpackage wrapper -> .exe / installer
│   └── package-mac.sh             jpackage wrapper -> .app / .dmg / .pkg
├── resource/images/               Toolbar icons (bundled into the jar)
└── src/main/java/com/unhuman/graphicv/
    ├── GraphiCV.java              Main class: UI, layout, all editing operations,
    │                              hex generation and parsing
    ├── JPixelButton.java          The one button type used for pixels, previews,
    │                              and color swatches; owns the TMS9918A palette
    ├── ColorChooserDialog.java    Modal 16-color palette picker
    ├── StoredImageState.java      Snapshot of one layer (pixels + colors) used to
    │                              save/restore when switching levels
    └── SpringUtilities.java       Oracle's SpringLayout helper (used for the
                                   label/field grids)
```

Buttons are located by generated string names (`pixel-<quadrant>-<x>-<y>`,
`color-<quadrant>-<row>-<STATE>`, `mastercolor-…`, `preview-<level>-…`) and looked up in
the `pixelButtonsMap` / `colorButtonsMap` maps on `GraphiCV`. `JPixelButton`'s static
name generators are the single source of truth for that scheme — start there when
reading the code.

---

## Known limitations

- **No file save or load.** Your work lives in the hex fields; copy them out before
  closing. Layers persist only for the life of the session.
- **Sprites are single-colored** (a hardware limitation, not a GraphiCV one). Per-row
  color editing is only meaningful on the `Background` layer.
- **Rotation discards per-row colors**, since rotating a row-colored image has no
  faithful representation on the hardware.
- The `Sets:` label at the bottom right is a placeholder for an unimplemented feature.
- Native builds are unsigned. macOS requires a right-click → *Open* on first launch, and
  Windows SmartScreen may warn until the executable earns reputation or you sign it.

---

## License and credits

GraphiCV by Howard Uman / [unHUman.com](https://unhuman.com).
`SpringUtilities.java` is Oracle's sample class from the Java Tutorial.
