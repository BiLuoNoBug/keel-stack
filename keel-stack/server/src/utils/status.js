/**
 * 状态机工具。
 *
 * 业务里凡是「几个固定状态 + 合法流转」的场景都适用（订单、工单、审批……）。
 * 约定：数据库只存英文枚举，接口同时下发中文文案，前端不用维护映射表。
 *
 * 用法：
 *   export const ITEM_STATUS = createStateMachine({
 *     text: { TODO: '待处理', DOING: '进行中', DONE: '已完成' },
 *     transitions: { TODO: ['DOING'], DOING: ['DONE'], DONE: [] }
 *   })
 *   ITEM_STATUS.text('DOING')          // '进行中'
 *   ITEM_STATUS.assert('TODO', 'DONE') // 不合法时抛错
 */
export function createStateMachine({ text, transitions = {} }) {
  const keys = Object.keys(text)

  return {
    text: (status) => text[status] ?? status,
    labels: text,
    keys,
    isValid: (status) => keys.includes(status),
    allowed: (from) => transitions[from] ?? [],
    /** 合法则放行，否则抛业务异常（交给 error 中间件转 400） */
    assert(from, to) {
      if (!keys.includes(to)) throw new Error(`未知状态：${to}`)
      const allowed = transitions[from] ?? []
      if (!allowed.includes(to)) {
        throw new Error(`状态不能从「${text[from] ?? from}」变成「${text[to] ?? to}」`)
      }
    }
  }
}
