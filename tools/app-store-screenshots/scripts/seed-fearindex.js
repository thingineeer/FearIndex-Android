const fs = require("fs");
const path = require("path");

const toolRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(toolRoot, "..", "..");
const metadataRoot = path.join(repoRoot, "fastlane", "metadata", "android");

const localeMap = [
  ["en", "en_US"],
  ["ko", "ko_KR"],
];

const buckets = [
  ["phone", "phoneScreenshots"],
  ["tablet7", "sevenInchScreenshots"],
  ["tablet10", "tenInchScreenshots"],
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function copyScreenshotSet(locale, supplyLocale, deviceName, fastlaneBucket) {
  const srcDir = path.join(metadataRoot, supplyLocale, "images", fastlaneBucket);
  const destDir = path.join(toolRoot, "public", "screenshots", "android", deviceName, locale);
  ensureDir(destDir);

  const files = ["1_notification.png", "2_home.png", "3_chart.png", "4_vote.png", "5_notification_settings.png"];
  for (const file of files) {
    const src = path.join(srcDir, file);
    if (!fs.existsSync(src)) continue;
    fs.copyFileSync(src, path.join(destDir, file));
  }
}

for (const [locale, supplyLocale] of localeMap) {
  for (const [deviceName, fastlaneBucket] of buckets) {
    copyScreenshotSet(locale, supplyLocale, deviceName, fastlaneBucket);
  }
}

const t = (en, ko) => ({ en, ko });

function phoneSlides(base) {
  return [
    {
      id: `${base}-home`,
      layout: "hero",
      label: t("MARKET FEAR", "시장 공포"),
      headline: t("Read market fear\nat a glance.", "시장 심리를\n한눈에 확인하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/2_home.png`,
    },
    {
      id: `${base}-kospi`,
      layout: "device-bottom",
      label: t("KOSPI ADDED", "코스피 추가"),
      headline: t("Track KOSPI fear\nbeside global markets.", "코스피 공포지수를\n글로벌 시장과 함께."),
      screenshot: `/screenshots/android/${base}/{locale}/3_chart.png`,
      inverted: true,
    },
    {
      id: `${base}-vote`,
      layout: "two-devices",
      label: t("COMMUNITY SIGNAL", "투표 신호"),
      headline: t("See how many investors\nfeel stuck.", "물린 투자자들의\n체감 심리를 확인하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/4_vote.png`,
      screenshotSecondary: `/screenshots/android/${base}/{locale}/2_home.png`,
    },
    {
      id: `${base}-alerts`,
      layout: "device-top",
      label: t("THRESHOLD ALERTS", "임계값 알림"),
      headline: t("Set alerts for fear,\ngreed, crypto and KOSPI.", "공포, 탐욕, 암호화폐,\n코스피 알림을 설정하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/5_notification_settings.png`,
    },
    {
      id: `${base}-push`,
      layout: "device-bottom",
      label: t("FAST NOTICES", "빠른 알림"),
      headline: t("Catch major sentiment\nmoves as they happen.", "중요한 심리 변화를\n놓치지 마세요."),
      screenshot: `/screenshots/android/${base}/{locale}/1_notification.png`,
      inverted: true,
    },
  ];
}

function tabletSlides(base) {
  return [
    {
      id: `${base}-home`,
      layout: "hero",
      label: t("MARKET FEAR", "시장 공포"),
      headline: t("Read market fear\nat a glance.", "시장 심리를\n한눈에 확인하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/2_home.png`,
    },
    {
      id: `${base}-kospi`,
      layout: "device-bottom",
      label: t("KOSPI ADDED", "코스피 추가"),
      headline: t("Track KOSPI fear\nbeside global markets.", "코스피 공포지수를\n글로벌 시장과 함께."),
      screenshot: `/screenshots/android/${base}/{locale}/3_chart.png`,
      inverted: true,
    },
    {
      id: `${base}-vote`,
      layout: "two-devices",
      label: t("COMMUNITY SIGNAL", "투표 신호"),
      headline: t("See how many investors\nfeel stuck.", "물린 투자자들의\n체감 심리를 확인하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/4_vote.png`,
      screenshotSecondary: `/screenshots/android/${base}/{locale}/2_home.png`,
    },
    {
      id: `${base}-push`,
      layout: "device-top",
      label: t("THRESHOLD ALERTS", "임계값 알림"),
      headline: t("Set KOSPI and crypto\nthreshold alerts.", "코스피와 암호화폐\n임계값 알림을 설정하세요."),
      screenshot: `/screenshots/android/${base}/{locale}/1_notification.png`,
    },
  ];
}

const state = {
  schemaVersion: 2,
  appName: "FearIndex",
  themeId: "ocean-fresh",
  connectedCanvas: false,
  locales: ["en", "ko"],
  locale: "en",
  device: "android",
  orientation: "portrait",
  appIcon: "/app-icon.png",
  slidesByDevice: {
    iphone: [],
    ipad: [],
    android: phoneSlides("phone"),
    "android-7": tabletSlides("tablet7"),
    "android-10": tabletSlides("tablet10"),
    "feature-graphic": [
      {
        id: "feature-graphic",
        layout: "feature-graphic",
        label: {},
        headline: t(
          "Market fear, KOSPI and crypto sentiment in one view.",
          "시장 공포, 코스피, 암호화폐 심리를 한눈에.",
        ),
        screenshot: "",
      },
    ],
  },
};

fs.writeFileSync(
  path.join(toolRoot, "app-store-screenshots.json"),
  `${JSON.stringify(state, null, 2)}\n`,
);

console.log("Seeded FearIndex screenshots project.");
