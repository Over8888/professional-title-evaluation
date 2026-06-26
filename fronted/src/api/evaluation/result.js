import request, { download } from '@/utils/request'

export function calculate(activityId) {
  return request({
    url: '/evaluation/result/calculate/' + activityId,
    method: 'post'
  })
}

export function summary(activityId) {
  return request({
    url: '/evaluation/result/summary/' + activityId,
    method: 'get'
  })
}

export function list(query) {
  return request({
    url: '/evaluation/result/list',
    method: 'get',
    params: query
  })
}

export function exportResult(query, filename) {
  return download('evaluation/result/export', query, filename || `evaluation_result_${new Date().getTime()}.xlsx`)
}
