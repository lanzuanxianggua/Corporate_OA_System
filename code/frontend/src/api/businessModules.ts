import request from "@/utils/request";

export interface PageQuery {
  pn?: number;
  ps?: number;
  pageNum?: number;
  pageSize?: number;
  [key: string]: string | number | undefined;
}

export const supplyApi = {
  list: (params: PageQuery) => request.get("/api/admin/supplies", { params }),
  create: (data: Record<string, unknown>) => request.post("/api/admin/supplies", data),
  update: (id: number, data: Record<string, unknown>) => request.put(`/api/admin/supplies/${id}`, data),
  categories: () => request.get("/api/admin/supplies/categories"),
  createCategory: (data: Record<string, unknown>) => request.post("/api/admin/supplies/categories", data),
  stock: (id: number) => request.get(`/api/admin/supplies/${id}/stock`),
  adjustStock: (id: number, data: Record<string, unknown>) => request.post(`/api/admin/supplies/${id}/stock-adjustments`, data),
  requests: (params: PageQuery) => request.get("/api/admin/supplies/requests", { params }),
  createRequest: (data: Record<string, unknown>) => request.post("/api/admin/supplies/requests", data),
  approveRequest: (id: number) => request.post(`/api/admin/supplies/requests/${id}/approve`),
  rejectRequest: (id: number, reason: string) => request.post(`/api/admin/supplies/requests/${id}/reject`, { reason })
};

export const performanceApi = {
  goals: (params: PageQuery) => request.get("/api/hr-performance/goals", { params }),
  createGoal: (data: Record<string, unknown>) => request.post("/api/hr-performance/goals", data),
  submitGoal: (id: number) => request.post(`/api/hr-performance/goals/${id}/submit`),
  evals: (params: PageQuery) => request.get("/api/hr-performance/evals", { params }),
  createEval: (data: Record<string, unknown>) => request.post("/api/hr-performance/evals", data),
  submitEval: (id: number) => request.post(`/api/hr-performance/evals/${id}/submit`),
  results: (params: PageQuery) => request.get("/api/hr-performance/results", { params }),
  generateResults: (cycleId: number) => request.post("/api/hr-performance/results/generate", null, { params: { cycleId } })
};

export const recruitmentApi = {
  jobs: (params: PageQuery) => request.get("/api/hr-recruitment/jobs", { params }),
  createJob: (data: Record<string, unknown>) => request.post("/api/hr-recruitment/jobs", data),
  candidates: (params: PageQuery) => request.get("/api/hr-recruitment/candidates", { params }),
  createCandidate: (data: Record<string, unknown>) => request.post("/api/hr-recruitment/candidates", data),
  interviews: (params: PageQuery) => request.get("/api/hr-recruitment/interviews", { params }),
  createInterview: (data: Record<string, unknown>) => request.post("/api/hr-recruitment/interviews", data),
  updateInterview: (id: number, data: Record<string, unknown>) => request.put(`/api/hr-recruitment/interviews/${id}`, data),
  offers: (params: PageQuery) => request.get("/api/hr-recruitment/offers", { params }),
  createOffer: (data: Record<string, unknown>) => request.post("/api/hr-recruitment/offers", data),
  acceptOffer: (id: number) => request.post(`/api/hr-recruitment/offers/${id}/accept`),
  onboardOffer: (id: number) => request.post(`/api/hr-recruitment/offers/${id}/onboard`)
};

export const trainingApi = {
  courses: (params: PageQuery) => request.get("/api/hr-training/courses", { params }),
  createCourse: (data: Record<string, unknown>) => request.post("/api/hr-training/courses", data),
  plans: (params: PageQuery) => request.get("/api/hr-training/plans", { params }),
  createPlan: (data: Record<string, unknown>) => request.post("/api/hr-training/plans", data),
  publishPlan: (id: number) => request.post(`/api/hr-training/plans/${id}/publish`),
  sessions: (params: PageQuery) => request.get("/api/hr-training/sessions", { params }),
  createSession: (data: Record<string, unknown>) => request.post("/api/hr-training/sessions", data),
  startSession: (id: number) => request.post(`/api/hr-training/sessions/${id}/start`),
  closeSession: (id: number) => request.post(`/api/hr-training/sessions/${id}/close`),
  enrollments: (params: PageQuery) => request.get("/api/hr-training/enrollments", { params }),
  enroll: (data: Record<string, unknown>) => request.post("/api/hr-training/enrollments", data),
  signIn: (id: number) => request.post(`/api/hr-training/enrollments/${id}/sign-in`),
  score: (id: number, score: number) => request.post(`/api/hr-training/enrollments/${id}/score`, { score }),
  records: (params: PageQuery) => request.get("/api/hr-training/enrollments/records", { params })
};

export const employeeExtraApi = {
  contracts: (empId: number, params: PageQuery) => request.get(`/api/hr/employees/${empId}/contracts`, { params }),
  createContract: (empId: number, data: Record<string, unknown>) => request.post(`/api/hr/employees/${empId}/contracts`, data),
  changes: (empId: number, params: PageQuery) => request.get(`/api/hr/employees/${empId}/changes`, { params }),
  createChange: (empId: number, data: Record<string, unknown>) => request.post(`/api/hr/employees/${empId}/changes`, data),
  certificates: (empId: number, params: PageQuery) => request.get(`/api/hr/employees/${empId}/certificates`, { params }),
  createCertificate: (empId: number, data: Record<string, unknown>) => request.post(`/api/hr/employees/${empId}/certificates`, data),
  educations: (empId: number, params: PageQuery) => request.get(`/api/hr/employees/${empId}/educations`, { params }),
  createEducation: (empId: number, data: Record<string, unknown>) => request.post(`/api/hr/employees/${empId}/educations`, data)
};

export const financeContractApi = {
  contracts: (params: PageQuery) => request.get("/api/finance/contracts", { params }),
  createContract: (data: Record<string, unknown>) => request.post("/api/finance/contracts", data),
  activateContract: (id: number) => request.post(`/api/finance/contracts/${id}/activate`),
  closeContract: (id: number) => request.post(`/api/finance/contracts/${id}/close`),
  payments: (params: PageQuery) => request.get("/api/finance/payments", { params }),
  createPayment: (data: Record<string, unknown>) => request.post("/api/finance/payments", data),
  submitPayment: (id: number) => request.post(`/api/finance/payments/${id}/submit`),
  markPaid: (id: number, payMethod: string) => request.post(`/api/finance/payments/${id}/paid`, { payMethod })
};

export const knowledgeApi = {
  categories: () => request.get("/api/knowledge/categories/tree"),
  createCategory: (data: Record<string, unknown>) => request.post("/api/knowledge/categories", data),
  entries: (params: PageQuery) => request.get("/api/knowledge/entries", { params }),
  createEntry: (data: Record<string, unknown>) => request.post("/api/knowledge/entries", data),
  publishEntry: (id: number) => request.post(`/api/knowledge/entries/${id}/actions/publish`),
  archiveEntry: (id: number) => request.post(`/api/knowledge/entries/${id}/actions/archive`)
};

export const taskCollabApi = {
  projects: (params: PageQuery) => request.get("/api/task/projects", { params }),
  createProject: (data: Record<string, unknown>) => request.post("/api/task/projects", data),
  projectStatus: (id: number, status: string) => request.put(`/api/task/projects/${id}/actions/status`, { status }),
  items: (params: PageQuery) => request.get("/api/task/items", { params }),
  createItem: (data: Record<string, unknown>) => request.post("/api/task/items", data),
  itemStatus: (id: number, status: string) => request.put(`/api/task/items/${id}/actions/status`, { status }),
  itemProgress: (id: number, progress: number) => request.put(`/api/task/items/${id}/actions/progress`, { progress })
};
