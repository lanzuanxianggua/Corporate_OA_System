# DB Schema vs Java Entity 一致性报告

**生成时间**: Mon Jun 08 11:33:51 CST 2026

- DB 表总数: **54**
- Entity 总数: **48**

## SysDept (table: sys_dept)
**Entity 字段但 DB 无** (3):
- `children`
- `phone`
- `sort`

## SysDictData (table: sys_dict_data)
**Entity 字段但 DB 无** (1):
- `id`
**DB 字段但 Entity 无** (1):
- `data_id`

## OaPurchase (table: oa_purchase)
**Entity 字段但 DB 无** (2):
- `emp_name`
- `remark`

## OaOuting (table: oa_outing)
**Entity 字段但 DB 无** (2):
- `emp_name`
- `remark`

## OaSalaryRecord (table: oa_salary_record)
**Entity 字段但 DB 无** (1):
- `emp_name`

## OaLoan (table: oa_loan)
**Entity 字段但 DB 无** (1):
- `emp_name`

## OaNotice (table: oa_notice)
**Entity 字段但 DB 无** (4):
- `is_read`
- `notice_type`
- `publisher`
- `publisher_id`
**DB 字段但 Entity 无** (1):
- `is_top`

## SysDictType (table: sys_dict_type)
**Entity 字段但 DB 无** (1):
- `id`
**DB 字段但 Entity 无** (1):
- `dict_id`

## OaLeaveApply (table: oa_leave_apply)
**Entity 字段但 DB 无** (3):
- `emp_name`
- `leave_period`
- `remark`

## WfDelegation (table: wf_delegation)
**Entity 字段但 DB 无** (4):
- `business_type`
- `delegate_to_id`
- `end_time`
- `start_time`
**DB 字段但 Entity 无** (4):
- `end_date`
- `notify_delegator`
- `process_category`
- `start_date`

## OaAssetBorrow (table: oa_asset_borrow)
**Entity 字段但 DB 无** (2):
- `asset_name`
- `borrower`

## SysRole (table: sys_role)
**Entity 字段但 DB 无** (1):
- `sort`

## OaExpense (table: oa_expense)
**Entity 字段但 DB 无** (2):
- `emp_name`
- `remark`

## OaApprovalRecord (table: oa_approval_record)
**Entity 字段但 DB 无** (1):
- `assignee_name`

## OaBusinessTrip (table: oa_business_trip)
**Entity 字段但 DB 无** (2):
- `emp_name`
- `remark`

## SysConfig (table: sys_config)
**Entity 字段但 DB 无** (1):
- `id`
**DB 字段但 Entity 无** (1):
- `config_id`

## OaEmpArchive (table: oa_emp_archive)
**Entity 字段但 DB 无** (7):
- `dept_name`
- `email`
- `emp_name`
- `emp_no`
- `hire_date`
- `id_card`
- `phone`

## OaDocument (table: oa_document)
**Entity 字段但 DB 无** (7):
- `description`
- `doc_name`
- `download_count`
- `file_path`
- `file_size`
- `file_type`
- `uploader_id`

## OaMeeting (table: oa_meeting)
**Entity 字段但 DB 无** (2):
- `organizer_name`
- `room_name`

## OaMessage (table: oa_message)
**Entity 字段但 DB 无** (2):
- `msg_type`
- `sender_name`

## OaSalaryStructure (table: oa_salary_structure)
**Entity 字段但 DB 无** (1):
- `emp_name`

## WfTask (table: wf_task)
**Entity 字段但 DB 无** (14):
- `action_source`
- `action_time`
- `assignee_name`
- `business_title`
- `business_type`
- `deadline`
- `instance`
- `last_remind_time`
- `multi_type`
- `node_index`
- `process_id`
- `remark`
- `transfer_from_id`
- `transfer_reason`
**DB 字段但 Entity 无** (6):
- `complete_time`
- `due_time`
- `node_id`
- `opinion`
- `signature`
- `task_type`

## OaSchedule (table: oa_schedule)
**Entity 字段但 DB 无** (1):
- `remind_time`

## OaLeaveBalance (table: oa_leave_balance)
**Entity 字段但 DB 无** (2):
- `dept_name`
- `emp_name`

## SysPost (table: sys_post)
**Entity 字段但 DB 无** (1):
- `id`
**DB 字段但 Entity 无** (1):
- `post_id`

## OaOvertime (table: oa_overtime)
**Entity 字段但 DB 无** (1):
- `emp_name`

## WfCcRecord (table: wf_cc_record)
**Entity 字段但 DB 无** (1):
- `cc_emp_id`

## Entity 对应 DB 表不存在

## DB 表无对应 Java Entity
- `wf_assignee_rule`
- `wf_definition`
- `wf_instance`
- `wf_node`
- `wf_record`
- `wf_transition`

## 汇总
- 实体匹配 DB 表: 48
- Entity 无对应表: 0
- Entity 字段多出 (DB 无): 70
- DB 字段多出 (Entity 无): 15
