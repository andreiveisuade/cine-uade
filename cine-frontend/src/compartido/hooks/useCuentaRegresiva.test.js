import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useCuentaRegresiva } from "./useCuentaRegresiva.js";

describe("useCuentaRegresiva", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("baja de a un segundo y avisa una sola vez al llegar a cero", () => {
    const alTerminar = vi.fn();
    const { result } = renderHook(() => useCuentaRegresiva(2, alTerminar));
    expect(result.current).toBe(2);

    act(() => vi.advanceTimersByTime(1000));
    expect(result.current).toBe(1);
    expect(alTerminar).not.toHaveBeenCalled();

    act(() => vi.advanceTimersByTime(5000));
    expect(result.current).toBe(0);
    expect(alTerminar).toHaveBeenCalledTimes(1);
  });
});
