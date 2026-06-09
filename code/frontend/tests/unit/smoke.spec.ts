import { beforeEach, describe, expect, it, vi } from "vitest";
import { performanceApi, supplyApi } from "../../src/api/businessModules";

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock("@/utils/request", () => ({
  default: requestMock,
}));

describe("business module APIs", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("calls the supply list endpoint with paging parameters", () => {
    const params = { pageNum: 1, pageSize: 10 };

    supplyApi.list(params);

    expect(requestMock.get).toHaveBeenCalledWith("/api/admin/supplies", { params });
  });

  it("calls the performance result generation endpoint", () => {
    performanceApi.generateResults(3);

    expect(requestMock.post).toHaveBeenCalledWith(
      "/api/hr-performance/results/generate",
      null,
      { params: { cycleId: 3 } },
    );
  });
});
