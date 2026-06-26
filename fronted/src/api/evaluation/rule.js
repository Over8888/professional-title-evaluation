import request from '@/utils/request'

export function getRuleConfig(activityId) {
  return request({
    url: '/evaluation/rule/activity/' + activityId,
    method: 'get'
  })
}

export function saveRuleConfig(data) {
  return request({
    url: '/evaluation/rule',
    method: 'post',
    data
  })
}

export function updateRuleConfig(data) {
  return request({
    url: '/evaluation/rule',
    method: 'put',
    data
  })
}

export function previewRule(activityId, query) {
  return request({
    url: '/evaluation/rule/preview/' + activityId,
    method: 'get',
    params: query
  })
}

export function applyRule(activityId) {
  return request({
    url: '/evaluation/rule/apply/' + activityId,
    method: 'put'
  })
}

export function applyConfirmedRule(activityId, data) {
  return request({
    url: '/evaluation/rule/apply/' + activityId + '/confirmed',
    method: 'put',
    data
  })
}

export function listRegistrationStats(query) {
  return request({
    url: '/evaluation/rule/registrationStats',
    method: 'get',
    params: query
  })
}

export function listRangePreview(query) {
  return request({
    url: '/evaluation/rule/rangePreview',
    method: 'get',
    params: query
  })
}
