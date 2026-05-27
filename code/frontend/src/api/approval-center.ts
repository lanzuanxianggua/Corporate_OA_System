import { getLeavePage, approveLeave } from "@/api/leave";
import { getBusinessTripPage, approveBusinessTrip } from "@/api/businessTrip";
import { getOutingPage, approveOuting } from "@/api/outing";
import { getPurchasePage, approvePurchase } from "@/api/purchase";
import { getExpensePage, approveExpense } from "@/api/expense";
import { getOvertimePage, approveOvertime } from "@/api/overtime";
import { getLoanPage, approveLoan } from "@/api/loan";

export interface ApprovalTypeConfig {
  label: string;
  businessType: string;
  getPage: (params: any) => any;
  approve: (data: any) => any;
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
