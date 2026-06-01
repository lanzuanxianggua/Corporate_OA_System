/** API unified response wrapper */
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

/** Paginated list result */
export interface PageResult<T> {
  list: T[];
  total: number;
}

/** Pagination query params */
export interface PageParams {
  page?: number;
  size?: number;
}

// ==================== Attendance ====================

export interface Attendance {
  id: number;
  empId: number;
  workDate: string;
  clockIn: string;
  clockOut: string;
  status: number;
  remark?: string;
  address?: string;
}

// ==================== Leave ====================

export interface LeaveApply {
  id: number;
  empId: number;
  leaveType: string;
  startTime: string;
  endTime: string;
  reason: string;
  days: number;
  status: number;
  leavePeriod?: string;
  createTime: string;
  empName?: string;
  remark?: string;
  processInstanceId?: number;
}

export interface LeaveBalance {
  id: number;
  empId: number;
  leaveType: number;
  year: number;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
  empName?: string;
  deptName?: string;
}

export interface SubmitLeaveParams {
  leaveType: string;
  startTime: string;
  endTime: string;
  reason: string;
  days: number;
  leavePeriod?: string;
  remark?: string;
}

// ==================== Business Trip ====================

export interface BusinessTrip {
  id: number;
  empId: number;
  destination: string;
  purpose: string;
  startTime: string;
  endTime: string;
  status: number;
  createTime: string;
  empName?: string;
  remark?: string;
  processInstanceId?: number;
}

export interface SubmitBusinessTripParams {
  destination: string;
  purpose: string;
  startTime: string;
  endTime: string;
  remark?: string;
}

// ==================== Outing ====================

export interface Outing {
  id: number;
  empId: number;
  reason: string;
  destination: string;
  startTime: string;
  endTime: string;
  status: number;
  createTime: string;
  empName?: string;
  remark?: string;
  processInstanceId?: number;
}

export interface SubmitOutingParams {
  reason: string;
  destination: string;
  startTime: string;
  endTime: string;
  remark?: string;
}

// ==================== Overtime ====================

export interface Overtime {
  id: number;
  empId: number;
  overtimeDate: string;
  startTime: string;
  endTime: string;
  hours: number;
  reason: string;
  status: number;
  createTime: string;
  empName?: string;
  processInstanceId?: number;
}

export interface SubmitOvertimeParams {
  overtimeDate: string;
  startTime: string;
  endTime: string;
  hours: number;
  reason: string;
}

// ==================== Purchase ====================

export interface Purchase {
  id: number;
  empId: number;
  itemName: string;
  quantity: number;
  amount: number;
  reason: string;
  status: number;
  createTime: string;
  empName?: string;
  remark?: string;
  processInstanceId?: number;
}

export interface SubmitPurchaseParams {
  itemName: string;
  quantity: number;
  amount: number;
  reason: string;
  remark?: string;
}

// ==================== Expense ====================

export interface Expense {
  id: number;
  empId: number;
  title: string;
  amount: number;
  category: string;
  description?: string;
  status: number;
  createTime: string;
  empName?: string;
  remark?: string;
  processInstanceId?: number;
}

export interface SubmitExpenseParams {
  title: string;
  amount: number;
  category: string;
  description?: string;
  remark?: string;
}

// ==================== Loan ====================

export interface Loan {
  id: number;
  empId: number;
  loanAmount: number;
  loanReason: string;
  repaymentPlan?: string;
  status: number;
  createTime: string;
  empName?: string;
  processInstanceId?: number;
}

export interface SubmitLoanParams {
  loanAmount: number;
  loanReason: string;
  repaymentPlan?: string;
}

// ==================== Workflow ====================

export interface WfProcessDefinition {
  id: number;
  processName: string;
  processKey: string;
  processType: string;
  nodeConfig: string;
  status: number;
  version: number;
  createTime: string;
}

export interface WfProcessInstance {
  id: number;
  processId: number;
  businessType: string;
  businessId: number;
  initiatorId: number;
  currentNode: number;
  status: number;
  startTime: string;
  endTime?: string;
  createTime: string;
  parentInstanceId?: number;
}

export interface WorkflowTask {
  id: number;
  instanceId: number;
  processId: number;
  nodeIndex: number;
  nodeName: string;
  assigneeId: number;
  /** 0-pending 1-approved 2-rejected 3-transferred 4-canceled 5-returned */
  status: number;
  actionTime?: string;
  remark?: string;
  createTime: string;
  businessTitle?: string;
  businessType?: string;
  assigneeName?: string;
  instance?: WfProcessInstance;
}

export interface HandleTaskParams {
  taskId: number;
  action: 'approve' | 'reject';
  remark?: string;
}

export interface TransferTaskParams {
  taskId: number;
  assigneeId: number;
  reason?: string;
}

export interface ReturnTaskParams {
  taskId: number;
  remark?: string;
}

export interface WithdrawParams {
  instanceId: number;
  remark?: string;
}

// ==================== Employee ====================

export interface Employee {
  id: number;
  empCode: string;
  empName: string;
  phone?: string;
  email?: string;
  deptId?: number;
  avatar?: string;
  status: number;
  gender?: string;
  hireDate?: string;
  createTime: string;
}

// ==================== Notice ====================

export interface Notice {
  id: number;
  title: string;
  content: string;
  noticeType: number;
  publisherId: number;
  status: number;
  createTime: string;
  publisher?: string;
  isRead?: boolean;
}

// ==================== Document ====================

export interface Document {
  id: number;
  docName: string;
  description?: string;
  filePath: string;
  fileSize?: number;
  fileType?: string;
  categoryId?: number;
  uploaderId: number;
  status: number;
  createTime: string;
}

// ==================== Message ====================

export interface Message {
  id: number;
  senderId: number;
  receiverId: number;
  title: string;
  content: string;
  isRead: number;
  createTime: string;
  senderName?: string;
}

export interface SendMessageParams {
  receiverId: number;
  title: string;
  content: string;
}

// ==================== Todo ====================

export interface Todo {
  id: number;
  empId: number;
  title: string;
  todoType: string;
  businessId?: number;
  businessType?: string;
  status: number;
  createTime: string;
  doneTime?: string;
}

// ==================== Schedule ====================

export interface Schedule {
  id: number;
  empId: number;
  title: string;
  content?: string;
  startTime: string;
  endTime: string;
  remindTime?: string;
  status: number;
  createTime: string;
}

export interface SubmitScheduleParams {
  title: string;
  content?: string;
  startTime: string;
  endTime: string;
  remindTime?: string;
}

// ==================== Report ====================

export interface AttendanceSummary {
  workDays: number;
  lateDays: number;
  earlyLeaveDays: number;
  absentDays: number;
  overtimeHours: number;
}

export interface LeaveSummary {
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

// ==================== Auth ====================

export interface CaptchaResult {
  uuid: string;
  image: string;
}

export interface LoginParams {
  username: string;
  password: string;
  captchaUuid: string;
  captchaCode: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  empId: number;
  empName: string;
  deptId?: number;
  avatar?: string;
}

export interface RefreshTokenResult {
  accessToken: string;
  refreshToken?: string;
}
