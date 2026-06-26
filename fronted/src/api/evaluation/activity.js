import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

export function listActivity(query) {
  return request({
    url: '/evaluation/activity/list',
    method: 'get',
    params: query
  })
}

export function getActivity(activityId) {
  return request({
    url: '/evaluation/activity/' + parseStrEmpty(activityId),
    method: 'get'
  })
}

export function addActivity(data) {
  return request({
    url: '/evaluation/activity',
    method: 'post',
    data
  })
}

export function createActivityWithRule(data) {
  return request({
    url: '/evaluation/activity/createWithRule',
    method: 'post',
    data
  })
}

export function updateActivity(data) {
  return request({
    url: '/evaluation/activity',
    method: 'put',
    data
  })
}

export function delActivity(activityId) {
  return request({
    url: '/evaluation/activity/' + activityId,
    method: 'delete'
  })
}

export function publishActivity(activityId) {
  return request({
    url: '/evaluation/activity/publish/' + activityId,
    method: 'put'
  })
}

export function closeActivity(activityId) {
  return request({
    url: '/evaluation/activity/close/' + activityId,
    method: 'put'
  })
}
