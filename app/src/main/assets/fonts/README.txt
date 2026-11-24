ERTHI NOTE — BUNDLED FONTS DIRECTORY
=====================================
Place OFL or Apache-2.0 licensed font files here for offline use.
The app works without these files (falls back to downloadable fonts
or system fonts), but bundling them removes the GMS/internet dependency.

Required files and their sources (all SIL OFL 1.1 or Apache 2.0):

  Amiri-Regular.ttf        — github.com/aliftype/amiri (OFL 1.1)
  Cairo-Regular.ttf        — github.com/google/fonts Cairo (OFL 1.1)
  NotoKufiArabic-Regular.ttf — github.com/notofonts (OFL 1.1)
  Tajawal-Regular.ttf      — github.com/googlefonts/tajawal (OFL 1.1)
  ReemKufi-Regular.ttf     — github.com/googlefonts/reemkufi (OFL 1.1)
  PlayfairDisplay-Regular.ttf — github.com/google/fonts Playfair Display (OFL)
  EBGaramond-Regular.ttf   — github.com/octaviopardo/EBGaramond12 (OFL 1.1)
  Montserrat-Regular.ttf   — github.com/JulietaUla/Montserrat (OFL 1.1)

Download script (requires curl):
  cd app/src/main/assets/fonts
  curl -L -o Cairo-Regular.ttf \
    "https://github.com/google/fonts/raw/main/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf"
  ... (see docs/DOWNLOAD_FONTS.sh for the full script)

Do NOT commit font files to version control without verifying the license.
