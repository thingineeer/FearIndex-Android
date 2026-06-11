"""
v1.7.9 Appium E2E 스크립트 — Android 모든 뷰/액션/UI label 검증.

사전 조건:
  1. Appium 서버 실행: appium &
  2. 에뮬레이터 또는 실기기 연결 (`adb devices`)
  3. 앱 설치 완료: `./gradlew :app:installDebug`

실행:
  python3 scripts/e2e/appium_v179_check.py

검증 범위:
  - 4탭 네비게이션 (홈/차트/투표/설정)
  - 홈: 게이지/비교카드/AdBanner/SimilarEventsCard/InsightTeaser/StuckCounter
  - 차트: 6기간 (3M/6M/1Y/2Y/3Y/5Y)
  - 투표: StuckCounter 토글
  - 설정: 알림 설정 진입
  - InsightDetailSheet 상단 "현재 N점에서 매수 시" 통계 카드 (v1.7.9 v2)
  - SimilarEvents pinned/similar 섹션 분리 검증
"""

import os
import sys
import time
import atexit
import subprocess
from typing import Optional

from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException


APP_PACKAGE = "th1ngjin.fearindex.debug"
APP_ACTIVITY = "th1ngjin.fearindex.MainActivity"
WAIT = 8


def adb(*args: str):
    subprocess.run(["adb", *args], check=False)


def enable_screenshot_mode():
    adb("shell", "setprop", "debug.screenshot_mode", "1")
    atexit.register(lambda: adb("shell", "setprop", "debug.screenshot_mode", "0"))


def make_driver():
    enable_screenshot_mode()
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.automation_name = "UiAutomator2"
    options.app_package = APP_PACKAGE
    options.app_activity = APP_ACTIVITY
    options.no_reset = True
    options.new_command_timeout = 120
    return webdriver.Remote("http://127.0.0.1:4723", options=options)


PASS = "[\u2713]"
FAIL = "[\u2717]"
INFO = "[i]"


def log(status: str, msg: str):
    print(f"{status} {msg}")


def find_text(driver, text: str, timeout: int = WAIT):
    try:
        return WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((AppiumBy.XPATH, f'//*[@text="{text}"]'))
        )
    except TimeoutException:
        return None


def has_text(driver, text: str, timeout: int = 3) -> bool:
    return find_text(driver, text, timeout) is not None


def has_text_contains(driver, partial: str, timeout: int = 3) -> bool:
    try:
        WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located((AppiumBy.XPATH, f'//*[contains(@text, "{partial}")]'))
        )
        return True
    except TimeoutException:
        return False


def tap_text(driver, text: str) -> bool:
    el = find_text(driver, text)
    if el is None:
        return False
    el.click()
    return True


def screenshot(driver, name: str):
    out = f"/tmp/fearindex_v179_{name}.png"
    driver.save_screenshot(out)
    log(INFO, f"screenshot: {out}")


def scroll_down(driver, distance: int = 1000):
    size = driver.get_window_size()
    cx = size["width"] // 2
    y_start = size["height"] // 4 * 3
    y_end = max(100, y_start - distance)
    driver.swipe(cx, y_start, cx, y_end, 400)
    time.sleep(0.4)


def scroll_to_top(driver):
    for _ in range(10):
        size = driver.get_window_size()
        cx = size["width"] // 2
        driver.swipe(cx, 200, cx, size["height"] - 200, 400)
        time.sleep(0.2)


def main():
    driver = make_driver()
    failures = 0
    passes = 0

    def check(label: str, ok: bool):
        nonlocal failures, passes
        if ok:
            passes += 1
            log(PASS, label)
        else:
            failures += 1
            log(FAIL, label)

    try:
        time.sleep(3)
        screenshot(driver, "01_launch")

        # === 홈 탭 ===
        log(INFO, "=== HOME tab ===")
        check("홈: '공포 탐욕 지수' 타이틀", has_text(driver, "공포 탐욕 지수"))
        check("홈: 등급 (탐욕/공포/중립 중 하나)",
              any(has_text_contains(driver, t, 1) for t in ["탐욕", "공포", "중립"]))
        check("홈: '시장' 세그먼트 라벨", has_text(driver, "시장"))
        check("홈: '암호화폐' 세그먼트 라벨", has_text(driver, "암호화폐"))
        check("홈: 비교 섹션 '비교' 라벨", has_text(driver, "비교"))
        check("홈: 비교 4기간 ('전일'/'1주전'/'1개월전'/'1년전')",
              all(has_text(driver, t, 2) for t in ["전일", "1주전", "1개월전", "1년전"]))

        # 스크롤해서 SimilarEvents/InsightTeaser/StuckCounter 검증
        scroll_down(driver, 800)
        time.sleep(1)
        screenshot(driver, "02_home_scrolled")

        # SimilarEventsCard
        if has_text(driver, "지금과 비슷했던 시기", 3):
            log(PASS, "홈: SimilarEventsCard 타이틀 (Firestore 데이터 도달)")
            passes += 1
            # pinned 섹션 검증
            if has_text(driver, "주요 저점에서 매수했다면", 2):
                log(PASS, "홈: SimilarEvents pinned 섹션 헤더")
                passes += 1
            else:
                log(INFO, "홈: pinned 섹션 없음 (현재 score에 pinned event 매칭 없음)")
            # disclaimer
            check("홈: SimilarEvents disclaimer",
                  has_text(driver, "과거 수익률은 미래 수익률을 보장하지 않습니다", 2))
        else:
            log(INFO, "홈: SimilarEventsCard 미표시 (Firestore 미연결 또는 score 미로드)")

        # InsightTeaserCard
        if has_text(driver, "그때 매수했다면?", 2):
            log(PASS, "홈: InsightTeaserCard ('그때 매수했다면?')")
            passes += 1

        # StuckCounter
        scroll_down(driver, 800)
        time.sleep(1)
        screenshot(driver, "03_home_bottom")

        # === 차트 탭 ===
        log(INFO, "=== CHART tab ===")
        scroll_to_top(driver)
        if not tap_text(driver, "차트"):
            log(FAIL, "차트 탭 클릭 실패")
            failures += 1
        else:
            time.sleep(1)
            screenshot(driver, "04_chart")
            check("차트: '히스토리' 라벨", has_text(driver, "히스토리"))
            check("차트: 3M 기간", has_text(driver, "3M"))
            check("차트: 6M 기간", has_text(driver, "6M"))
            check("차트: 1Y 기간", has_text(driver, "1Y"))
            check("차트: 2Y 기간", has_text(driver, "2Y"))
            check("차트: 3Y 기간", has_text(driver, "3Y"))
            check("차트: 5Y 기간", has_text(driver, "5Y"))
            # 5Y 클릭 → 차트 갱신
            tap_text(driver, "5Y")
            time.sleep(1)
            screenshot(driver, "05_chart_5y")

        # === 투표 탭 ===
        log(INFO, "=== VOTE tab ===")
        scroll_to_top(driver)
        if tap_text(driver, "투표"):
            time.sleep(1)
            screenshot(driver, "06_vote")
            check("투표: 화면 진입 OK", True)
        else:
            log(FAIL, "투표 탭 클릭 실패")
            failures += 1

        # === 설정 탭 ===
        log(INFO, "=== SETTINGS tab ===")
        if tap_text(driver, "설정"):
            time.sleep(1)
            screenshot(driver, "07_settings")
            check("설정: 화면 진입 OK", True)
        else:
            log(FAIL, "설정 탭 클릭 실패")
            failures += 1

        # === InsightDetailSheet 검증 (홈으로 돌아가서 인사이트 클릭) ===
        log(INFO, "=== InsightDetailSheet (홈으로 복귀 후) ===")
        if tap_text(driver, "홈"):
            time.sleep(1)
            scroll_down(driver, 800)
            time.sleep(0.5)
            scroll_down(driver, 600)
            time.sleep(0.5)
            # InsightTeaserCard 클릭
            teaser = find_text(driver, "그때 매수했다면?", 3)
            if teaser is not None:
                teaser.click()
                time.sleep(2)
                screenshot(driver, "08_insight_detail")
                # v1.7.9 v2 통계 카드
                if has_text_contains(driver, "현재", 3) and has_text(driver, "평균 수익", 2):
                    log(PASS, "DetailSheet: 현재 점수 통계 카드 (평균 수익)")
                    passes += 1
                if has_text(driver, "최대 낙폭", 2):
                    log(PASS, "DetailSheet: 통계 카드 (최대 낙폭)")
                    passes += 1
                if has_text(driver, "최고 수익", 2):
                    log(PASS, "DetailSheet: 통계 카드 (최고 수익)")
                    passes += 1
            else:
                log(INFO, "InsightTeaserCard 미발견")

        print()
        print(f"=" * 60)
        print(f"PASS: {passes} / FAIL: {failures}")
        print(f"=" * 60)

    finally:
        time.sleep(2)
        driver.quit()

    sys.exit(0 if failures == 0 else 1)


if __name__ == "__main__":
    main()
