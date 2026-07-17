#!/usr/bin/env python3
"""온보딩 코치마크 투어 E2E — adb UI 자동화 (uiautomator dump 로 텍스트 요소 탭).

검증:
  A. QA 강제 워크스루: 8단계 순회 + 각 단계 스크린샷 + 2단계 KOSPI 세그먼트 확인 + 종료 후 홈 랜딩
  B. 신규 설치 → 투어 자동 노출
  C. 기존 유저 업데이트 재현(onboarding_prefs만 삭제, deviceId 유지) → 미노출
  D. 투어 중 강제 종료 후 재실행 → 미노출

사전: 에뮬레이터 실행 + debug APK 설치(:app:installDebug).
실행: python3 scripts/e2e/onboarding_tour_check.py
"""
import os
import re
import subprocess
import sys
import time
import xml.dom.minidom as minidom

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
PKG = "th1ngjin.fearindex.debug"
ACT = "th1ngjin.fearindex.MainActivity"
OUT = os.path.join(os.path.dirname(__file__), "out", "onboarding")
os.makedirs(OUT, exist_ok=True)


def adb(*args, capture=False):
    cmd = [ADB, *args]
    if capture:
        return subprocess.run(cmd, capture_output=True, text=True).stdout
    subprocess.run(cmd, check=False)


def shell(cmd, capture=False):
    return adb("shell", cmd, capture=capture)


def force_stop():
    shell(f"am force-stop {PKG}")


def grant_notifications():
    # 알림 권한 프롬프트가 투어를 가리지 않도록 사전 허용 (실사용 게이팅은 코드에서 처리).
    shell(f"pm grant {PKG} android.permission.POST_NOTIFICATIONS")


def pm_clear():
    shell(f"pm clear {PKG}")
    grant_notifications()


def clear_onboarding_prefs_only():
    # 기존 유저 업데이트 재현: deviceId(stuck_counter_prefs)는 유지, onboarding_prefs만 삭제
    shell(f"run-as {PKG} rm -f shared_prefs/onboarding_prefs.xml")


def launch(qa=False, step=1):
    extras = ""
    if qa:
        extras = f" --ez qa_onboarding true --ei qa_onboarding_step {step}"
    shell(f"am start -n {PKG}/{ACT}{extras}")


def dump_ui():
    shell("uiautomator dump /sdcard/ui.xml")
    xml = shell("cat /sdcard/ui.xml", capture=True)
    return xml


def find_center_by_text(text, exact=False):
    """uiautomator XML 에서 text 를 가진 노드의 중심 좌표. 없으면 None."""
    xml = dump_ui()
    try:
        dom = minidom.parseString(xml)
    except Exception:
        return None
    best = None
    for node in dom.getElementsByTagName("node"):
        t = node.getAttribute("text") or ""
        desc = node.getAttribute("content-desc") or ""
        hay = f"{t}\n{desc}"
        match = (text == t or text == desc) if exact else (text in hay)
        if match:
            b = node.getAttribute("bounds")  # [l,t][r,b]
            m = re.findall(r"\d+", b)
            if len(m) == 4:
                l, tp, r, bt = map(int, m)
                best = ((l + r) // 2, (tp + bt) // 2)
    return best


def has_text(text):
    return find_center_by_text(text) is not None


def tap(xy):
    shell(f"input tap {xy[0]} {xy[1]}")


def screenshot(name):
    path = os.path.join(OUT, f"{name}.png")
    shell(f"screencap -p /sdcard/s.png")
    adb("pull", "/sdcard/s.png", path, capture=True)
    return path


def wait(sec=1.5):
    time.sleep(sec)


PASS, FAIL = [], []


def check(cond, msg):
    (PASS if cond else FAIL).append(msg)
    print(("  ✅ " if cond else "  ❌ ") + msg)


def scenario_A_walkthrough():
    print("\n[A] QA 강제 워크스루 (8단계 + 스크린샷)")
    force_stop()
    launch(qa=True, step=1)
    wait(4)
    # 투어 카드 확인 (1/8 뱃지 또는 건너뛰기)
    check(has_text("1/8") or has_text("건너뛰기") or has_text("Skip"),
          "1단계 투어 카드 노출")
    for step in range(1, 9):
        screenshot(f"step{step}")
        if step == 2:
            # KOSPI 세그먼트 자동 전환 확인 (세그먼트/게이지에 KOSPI 존재)
            check(has_text("KOSPI"), "2단계에서 KOSPI 세그먼트 자동 전환")
        if step < 8:
            btn = (find_center_by_text("다음") or find_center_by_text("Next"))
            check(btn is not None, f"{step}단계 '다음' 버튼 존재")
            if btn:
                tap(btn)
                wait(1.6)
        else:
            btn = (find_center_by_text("시작하기") or find_center_by_text("Start"))
            check(btn is not None, "8단계 '시작하기' 버튼 존재")
            if btn:
                tap(btn)
                wait(1.8)
    # 종료 후 홈 최상단(오버레이 걷힘): 투어 카드 사라짐 + 게이지 화면
    screenshot("after_finish")
    tour_gone = not (has_text("건너뛰기") or has_text("Skip"))
    check(tour_gone, "종료 후 오버레이 걷힘(홈 랜딩)")


def scenario_B_new_install():
    print("\n[B] 신규 설치 → 자동 노출")
    force_stop()
    pm_clear()
    launch()
    wait(5)  # 스플래시(1.5s) + 투어 시작 지연
    screenshot("B_new_install")
    check(has_text("건너뛰기") or has_text("Skip") or has_text("1/8"),
          "신규 설치 첫 실행에 투어 자동 노출")


def scenario_C_update_user():
    print("\n[C] 기존 유저 업데이트 재현 → 미노출")
    # B 직후 상태(신규 설치로 이미 실행됨 → deviceId 생성됨). onboarding_prefs만 삭제.
    force_stop()
    clear_onboarding_prefs_only()
    launch()
    wait(5)
    screenshot("C_update_user")
    check(not (has_text("건너뛰기") or has_text("Skip")),
          "기존 유저(deviceId 존재)는 투어 미노출")


def scenario_D_kill_restart():
    print("\n[D] 투어 중 강제 종료 후 재실행 → 미노출")
    force_stop()
    pm_clear()
    launch()
    wait(5)
    appeared = has_text("건너뛰기") or has_text("Skip") or has_text("1/8")
    check(appeared, "신규 설치로 투어 노출(D 전제)")
    force_stop()  # 투어 중 강제 종료
    launch()
    wait(5)
    screenshot("D_after_kill")
    check(not (has_text("건너뛰기") or has_text("Skip")),
          "강제 종료 후 재실행 시 미노출(hasSeenTour)")


def main():
    if PKG not in (shell("pm list packages", capture=True) or ""):
        print(f"앱 미설치: {PKG} — ./gradlew :app:installDebug 먼저", file=sys.stderr)
        sys.exit(1)
    shell("settings put global hide_error_dialogs 1")
    grant_notifications()  # 최초 실행(scenario A) 권한 다이얼로그 방지
    scenario_A_walkthrough()
    scenario_B_new_install()
    scenario_C_update_user()
    scenario_D_kill_restart()
    print(f"\n=== 결과: {len(PASS)} PASS / {len(FAIL)} FAIL ===")
    for f in FAIL:
        print("  FAIL:", f)
    print(f"스크린샷: {OUT}")
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
