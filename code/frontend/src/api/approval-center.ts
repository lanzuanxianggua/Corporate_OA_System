import { getLeavePage, approveLeave } from "@/api/leave";
import { getBusinessTripPage, approveBusinessTrip } from "@/api/businessTrip";
import { getOutingPage, approveOuting } from "@/api/outing";
import { getPurchasePage, approvePurchase } from "@/api/purchase";
import { getExpensePage, approveExpense } from "@/api/expense";
import { getOvertimePage, approveOvertime } from "@/api/overtime";
import { getLoanPage, approveLoan } from "@/api/loan";
import type { ApiResponse, PageResult, ApproveDTO } from "@/types/api";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type PageFn = (params: any) => Promise<any>;
type ApproveFn = (data: ApproveDTO) => Promise<ApiResponse<void>>;

export interface ApprovalTypeConfig {
  label: string;
  businessType: string;
  getPage: PageFn;
  approve: ApproveFn;
}

export const approvalTypeConfigs: Record<string, ApprovalTypeConfig> = {
  leave: {
    label: "请假",
    businessType: "leave",
    getPage: getLeavePage,
    approve: approveLeave
  },
  trip: {
    label: "出差",
    businessType: "trip",
    getPage: getBusinessTripPage,
    approve: approveBusinessTrip
  },
  outing: {
    label: "外出",
    businessType: "outing",
    getPage: getOutingPage,
    approve: approveOuting
  },
  purchase: {
    label: "采购",
    businessType: "purchase",
    getPage: getPurchasePage,
    approve: approvePurchase
  },
  expense: {
    label: "经费",
    businessType: "expense",
    getPage: getExpensePage,
    approve: approveExpense
  },
  overtime: {
    label: "加班",
    businessType: "overtime",
    getPage: getOvertimePage,
    approve: approveOvertime
  },
  loan: {
    label: "借支",
    businessType: "loan",
    getPage: getLoanPage,
    approve: approveLoan
  }
};
