/**
 * Central API type definitions for the Corporate OA System frontend.
 * Replaces ad-hoc 'any' types with proper interfaces across all API files.
 */

// ── Generic wrappers ──────────────────────────────────────────────────────

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

// ── System entities ───────────────────────────────────────────────────────

export interface Employee {
  id?: number;
  empCode?: string;
  empName?: string;
  password?: string;
  phone?: string;
  email?: string;
  deptId?: number;
  deptName?: string;
  avatar?: string;
  status?: number;
  postId?: number;
  postName?: string;
  roleName?: string;
  empId?: number;
  createTime?: string;
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

// ── Auth / User ───────────────────────────────────────────────────────────

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
  username?: string;
  nickname?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
}

export interface CaptchaVO {
  uuid: string;
  image: string;
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

// ── OA core ───────────────────────────────────────────────────────────────

export interface Notice {
  id?: number;
  title?: string;
  content?: string;
  noticeType?: number;
  publisherId?: number;
  publisher?: string;
  publisherName?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
  isRead?: boolean;
  readCount?: number;
}

export interface Message {
  id?: string | number;
  senderId?: string | number;
  senderName?: string;
  receiverId?: number;
  receiverName?: string;
  msgType?: number;
  title?: string;
  content?: string;
  isRead?: number;
  createTime?: string;
}

export interface Document {
  id?: number;
  docName?: string;
  description?: string;
  filePath?: string;
  fileSize?: number;
  fileType?: string;
  categoryId?: number;
  categoryName?: string;
  downloadCount?: number;
  uploaderId?: number;
  uploaderName?: string;
  uploadTime?: string;
  createTime?: string;
}

export interface Schedule {
  id?: number;
  empId?: number;
  empName?: string;
  title?: string;
  content?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  remindTime?: string;
  status?: number;
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

// ── Business / leave types ────────────────────────────────────────────────

export interface LeaveApply {
  id?: number;
  empId?: number;
  empName?: string;
  leaveType?: number;
  startTime?: string;
  endTime?: string;
  reason?: string;
  status?: number;
  leavePeriod?: number;
  remark?: string;
  createTime?: string;
}

export interface BusinessTrip {
  id?: number;
  empId?: number;
  empName?: string;
  destination?: string;
  purpose?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
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
  remark?: string;
  createTime?: string;
}

export interface Purchase {
  id?: number;
  empId?: number;
  empName?: string;
  itemName?: string;
  quantity?: number;
  amount?: number;
  reason?: string;
  status?: number;
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
  remark?: string;
  createTime?: string;
}

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

// ── Assets / contracts / budgets ──────────────────────────────────────────

export interface Asset {
  id?: number;
  assetCode?: string;
  assetName?: string;
  category?: string;
  specification?: string;
  purchaseDate?: string;
  purchasePrice?: number;
  status?: number | string;
  currentUserId?: number;
  deptId?: number;
}

export interface AssetBorrow {
  id?: number;
  assetId?: number;
  assetName?: string;
  borrowerId?: number;
  borrowerName?: string;
  borrowTime?: string;
  expectedReturn?: string;
  actualReturn?: string;
  status?: number;
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

// ── Meetings ──────────────────────────────────────────────────────────────

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

// ── Salary ────────────────────────────────────────────────────────────────

export interface SalaryStructure {
  id?: number;
  empId?: number;
  empName?: string;
  baseSalary?: number;
  postSalary?: number;
  meritSalary?: number;
  allowance?: number;
  effectiveDate?: string;
  status?: number | string;
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

// ── Attendance ────────────────────────────────────────────────────────────

export interface Attendance {
  id?: number;
  empId?: number;
  empName?: string;
  workDate?: string;
  clockIn?: string;
  clockOut?: string;
  status?: number;
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

// ── Workflow ──────────────────────────────────────────────────────────────

export interface WorkflowTask {
  id?: number;
  processInstanceId?: number;
  nodeId?: string;
  nodeName?: string;
  assigneeId?: number;
  assigneeName?: string;
  status?: number;
  businessId?: number;
  businessType?: string;
  processName?: string;
  createTime?: string;
}

export interface ProcessDefinition {
  id?: number;
  processName?: string;
  processKey?: string;
  processType?: string;
  nodeConfig?: string;
  status?: number;
  version?: number;
}

export interface ProcessInstance {
  id?: number;
  processDefId?: number;
  businessId?: number;
  businessType?: string;
  initiatorId?: number;
  initiatorName?: string;
  status?: number;
  currentNode?: string;
  createTime?: string;
}

export interface CcRecord {
  id?: number;
  processInstanceId?: number;
  taskId?: number;
  ccUserId?: number;
  ccUserName?: string;
  businessType?: string;
  businessId?: number;
  isRead?: number;
  createTime?: string;
}

export interface Delegation {
  id?: number;
  delegatorId?: number;
  delegatorName?: string;
  delegateToId?: number;
  delegateToName?: string;
  startTime?: string;
  endTime?: string;
  status?: number;
}

// ── Monitoring / logs ─────────────────────────────────────────────────────

export interface OperationLog {
  id?: number;
  empId?: number;
  empName?: string;
  operation?: string;
  method?: string;
  params?: string;
  module?: string;
  ip?: string;
  createTime?: string;
}

export interface LoginLog {
  id?: number;
  empName?: string;
  ip?: string;
  location?: string;
  browser?: string;
  os?: string;
  status?: number;
  message?: string;
  loginTime?: string;
}

export interface OnlineLog {
  id?: number;
  empName?: string;
  ip?: string;
  location?: string;
  browser?: string;
  os?: string;
  loginTime?: string;
}

// ── Alerts ────────────────────────────────────────────────────────────────

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

// ── Approval records ─────────────────────────────────────────────────────

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

// ── Employee archive ──────────────────────────────────────────────────────

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
