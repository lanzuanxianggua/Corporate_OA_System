/**
 * Central API type definitions for the Corporate OA System frontend.
 * Organized by domain: System, Auth, OA Core, Business, Workflow, Monitoring.
 * All interfaces match backend DTOs/VOs with camelCase naming.
 */

// ============================================================================
// Generic Wrappers
// ============================================================================

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface PageParams {
  pageNum: number;
  pageSize: number;
}

// ============================================================================
// System Domain
// ============================================================================

export interface Employee {
  id?: number;
  empCode?: string;
  empName?: string;
  password?: string;
  phone?: string;
  email?: string;
  deptId?: number;
  avatar?: string;
  status?: number;
  postId?: number;
  delFlag?: string;
  createBy?: string;
  updateBy?: string;
  createTime?: string;
  updateTime?: string;
}

export interface Dept {
  id?: number;
  deptName?: string;
  parentId?: number;
  sort?: number;
  leader?: string;
  phone?: string;
  status?: number;
  createTime?: string;
  children?: Dept[];
}

export interface Role {
  id?: number;
  roleName?: string;
  name?: string;
  roleKey?: string;
  code?: string;
  sort?: number;
  status?: number;
  remark?: string;
  menuIds?: number[];
  createTime?: string;
}

export interface Menu {
  id?: number;
  menuName?: string;
  parentId?: number;
  path?: string;
  component?: string;
  menuType?: number | string;
  perms?: string;
  icon?: string;
  sort?: number;
  orderNum?: number;
  children?: Menu[];
}

export interface Post {
  id?: number;
  postCode?: string;
  postName?: string;
  sort?: number;
  status?: number;
}

export interface DictType {
  id?: number;
  dictName?: string;
  dictType?: string;
  status?: number;
}

export interface DictData {
  id?: number;
  dictType?: string;
  dictLabel?: string;
  dictValue?: string;
  sort?: number;
  status?: number;
}

export interface Config {
  id?: number;
  configName?: string;
  configKey?: string;
  configValue?: string;
  status?: number;
}

// ============================================================================
// Auth / User Domain
// ============================================================================

export interface LoginDTO {
  username: string;
  password: string;
  captchaUuid: string;
  captchaCode: string;
}

export interface LoginVO {
  accessToken: string;
  refreshToken: string;
  expires: string;
  username: string;
  nickname: string;
  avatar: string;
  roles: string[];
  permissions: string[];
}

export interface CaptchaVO {
  uuid: string;
  img: string;
}

export interface UserVO {
  id?: number;
  username?: string;
  nickname?: string;
  empName?: string;
  phone?: string;
  email?: string;
  status?: number;
  avatar?: string;
  createTime?: string;
  dept?: { id: number; name: string };
  roles?: Array<{ id: number; name: string; code: string }>;
}

// ============================================================================
// OA Core Domain
// ============================================================================

export interface Attendance {
  id?: number;
  empId?: number;
  workDate?: string;
  clockIn?: string;
  clockOut?: string;
  status?: number;
  remark?: string;
  ip?: string;
  address?: string;
}

export interface AttendanceGroup {
  id?: number;
  groupName?: string;
  workStart?: string;
  workEnd?: string;
  lateThreshold?: number;
  status?: number;
  empCount?: number;
}

export interface Notice {
  id?: number;
  title?: string;
  content?: string;
  noticeType?: number;
  publisherId?: number;
  publisher?: string;
  publisherName?: string;
  status?: number;
  isRead?: boolean;
  readCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface Document {
  id?: number;
  docName?: string;
  description?: string;
  filePath?: string;
  fileSize?: number;
  fileType?: string;
  categoryId?: number;
  downloadCount?: number;
  uploaderId?: number;
  uploadTime?: string;
  createTime?: string;
}

export interface Schedule {
  id?: number;
  empId?: number;
  title?: string;
  content?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  remindTime?: string;
  status?: number;
  createTime?: string;
}

export interface Message {
  id?: number;
  senderId?: number;
  senderName?: string;
  receiverId?: number;
  msgType?: number;
  title?: string;
  content?: string;
  isRead?: number;
  createTime?: string;
}

export interface Todo {
  id?: number;
  empId?: number;
  title?: string;
  todoType?: number;
  businessId?: number;
  businessType?: string;
  status?: number;
  doneTime?: string;
  createTime?: string;
}

// ============================================================================
// Business Domain - Leave & Leave Balance
// ============================================================================

export interface LeaveApply {
  id?: number;
  empId?: number;
  empName?: string;
  deptName?: string;
  leaveType?: number;
  startTime?: string;
  endTime?: string;
  reason?: string;
  status?: number;
  processInstanceId?: number;
  leavePeriod?: string;
  remark?: string;
  createTime?: string;
}

export interface LeaveBalance {
  id?: number;
  empId?: number;
  empName?: string;
  leaveType?: number;
  totalDays?: number;
  usedDays?: number;
  remainDays?: number;
  year?: number;
}

// ============================================================================
// Business Domain - Trips & Outing
// ============================================================================

export interface BusinessTrip {
  id?: number;
  empId?: number;
  empName?: string;
  destination?: string;
  purpose?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

export interface Outing {
  id?: number;
  empId?: number;
  empName?: string;
  destination?: string;
  reason?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

// ============================================================================
// Business Domain - Purchase & Expense
// ============================================================================

export interface Purchase {
  id?: number;
  empId?: number;
  empName?: string;
  itemName?: string;
  quantity?: number;
  amount?: number;
  reason?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

export interface Expense {
  id?: number;
  empId?: number;
  empName?: string;
  title?: string;
  amount?: number;
  category?: string | number;
  description?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

// ============================================================================
// Business Domain - Overtime & Loan
// ============================================================================

export interface Overtime {
  id?: number;
  empId?: number;
  empName?: string;
  overtimeDate?: string;
  startTime?: string;
  endTime?: string;
  hours?: number;
  reason?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

export interface Loan {
  id?: number;
  empId?: number;
  empName?: string;
  loanAmount?: number;
  loanReason?: string;
  repaymentPlan?: string;
  status?: number;
  processInstanceId?: number;
  remark?: string;
  createTime?: string;
}

export interface LoanRepayment {
  id?: number;
  loanId?: number;
  amount?: number;
  repayDate?: string;
  remark?: string;
}

// ============================================================================
// Business Domain - Assets & Contracts & Budgets
// ============================================================================

export interface Asset {
  id?: number;
  assetCode?: string;
  assetName?: string;
  category?: string;
  specification?: string;
  purchaseDate?: string;
  purchasePrice?: number;
  status?: string;
  currentUserId?: number;
  deptId?: number;
}

export interface AssetBorrow {
  id?: number;
  assetId?: number;
  assetName?: string;
  borrowerId?: number;
  borrower?: string;
  borrowTime?: string;
  expectedReturn?: string;
  actualReturn?: string;
  status?: string;
  remark?: string;
  createTime?: string;
}

export interface Contract {
  id?: number;
  contractNo?: string;
  contractName?: string;
  contractType?: string;
  partyA?: string;
  partyB?: string;
  amount?: number;
  signDate?: string;
  startDate?: string;
  endDate?: string;
  status?: number;
  managerId?: number;
  fileUrl?: string;
}

export interface Budget {
  id?: number;
  deptId?: number;
  deptName?: string;
  budgetYear?: number;
  budgetMonth?: number;
  amount?: number;
  usedAmount?: number;
  status?: number;
}

// ============================================================================
// Business Domain - Meetings
// ============================================================================

export interface MeetingRoom {
  id?: number;
  roomName?: string;
  location?: string;
  capacity?: number;
  facilities?: string;
  status?: number;
}

export interface Meeting {
  id?: number;
  title?: string;
  roomId?: number;
  roomName?: string;
  organizerId?: number;
  organizerName?: string;
  startTime?: string;
  endTime?: string;
  participants?: string;
  status?: number;
}

// ============================================================================
// Business Domain - Salary
// ============================================================================

export interface SalaryStructure {
  id?: number;
  empId?: number;
  empName?: string;
  baseSalary?: number;
  postSalary?: number;
  meritSalary?: number;
  allowance?: number;
  effectiveDate?: string;
  status?: number;
}

export interface SalaryRecord {
  id?: number;
  empId?: number;
  empName?: string;
  salaryMonth?: string;
  baseSalary?: number;
  postSalary?: number;
  meritSalary?: number;
  allowance?: number;
  deduction?: number;
  actualAmount?: number;
  payTime?: string;
  status?: number;
}

export interface EmpArchive {
  id?: number;
  empId?: number;
  empName?: string;
  gender?: number;
  birthday?: string;
  idCard?: string;
  education?: string;
  graduateSchool?: string;
  major?: string;
  entryDate?: string;
  phone?: string;
  email?: string;
  address?: string;
  emergencyContact?: string;
  emergencyPhone?: string;
}

// ============================================================================
// Workflow Domain
// ============================================================================

export interface ProcessDefinition {
  id?: number;
  processName?: string;
  processKey?: string;
  processType?: string;
  nodeConfig?: string;
  status?: string;
  version?: number;
}

export interface ProcessInstance {
  id?: number;
  processId?: number;
  businessType?: string;
  businessId?: number;
  initiatorId?: number;
  initiatorName?: string;
  currentNode?: number;
  status?: string;
  conditionContext?: string;
  activeNodes?: string;
  snapshotNodeConfig?: string;
  parentInstanceId?: number;
  processVersion?: number;
  startTime?: string;
  endTime?: string;
  createTime?: string;
}

export interface WorkflowTask {
  id?: number;
  instanceId?: number;
  processId?: number;
  nodeIndex?: number;
  nodeName?: string;
  assigneeId?: number;
  assigneeName?: string;
  status?: string;
  actionTime?: string;
  remark?: string;
  actionSource?: string;
  transferFromId?: number;
  transferReason?: string;
  deadline?: string;
  remindCount?: number;
  parentTaskId?: number;
  multiType?: string;
  createTime?: string;
  businessTitle?: string;
  businessType?: string;
  instance?: ProcessInstance;
}

export interface CcRecord {
  id?: number;
  instanceId?: number;
  taskId?: number;
  ccEmpId?: number;
  status?: string;
  createTime?: string;
}

export interface Delegation {
  id?: number;
  delegatorId?: number;
  delegateToId?: number;
  startTime?: string;
  endTime?: string;
  status?: string;
}

export interface ApprovalRecord {
  id?: number;
  applyId?: number;
  businessType?: string;
  approverId?: number;
  approverName?: string;
  approveStatus?: number;
  remark?: string;
  approveTime?: string;
  taskId?: number;
  nodeName?: string;
}

// ============================================================================
// Monitoring / Logs Domain
// ============================================================================

export interface OperationLog {
  id?: number;
  empId?: number;
  empName?: string;
  operation?: string;
  method?: string;
  requestUrl?: string;
  params?: string;
  module?: string;
  ip?: string;
  costTime?: number;
  status?: number;
  createTime?: string;
}

export interface LoginLog {
  id?: number;
  username?: string;
  ip?: string;
  os?: string;
  browser?: string;
  status?: number;
  message?: string;
  loginTime?: string;
}

export interface OnlineUser {
  empId?: number;
  empName?: string;
  ip?: string;
  loginTime?: string;
}

// ============================================================================
// Alert Rules
// ============================================================================

export interface AlertRule {
  id?: number;
  ruleName?: string;
  metricName?: string;
  condition?: string;
  threshold?: number;
  duration?: number;
  level?: string | number;
  notifyType?: string;
  notifyTarget?: string;
  enabled?: number;
  createTime?: string;
}

export interface AlertLog {
  id?: number;
  ruleId?: number;
  ruleName?: string;
  metricValue?: number;
  threshold?: number;
  level?: string | number;
  message?: string;
  status?: number;
  createTime?: string;
}

// ============================================================================
// Approve DTO (for approval endpoints)
// ============================================================================

export interface ApproveDTO {
  id: number;
  status: number;
  remark?: string;
  taskId?: number;
}
