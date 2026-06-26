import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

export function listCandidate(query) {
  return request({
    url: '/evaluation/candidate/list',
    method: 'get',
    params: query
  })
}

export function listCandidateOptions() {
  return request({
    url: '/evaluation/candidate/options',
    method: 'get'
  })
}

export function getCandidate(candidateId) {
  return request({
    url: '/evaluation/candidate/' + parseStrEmpty(candidateId),
    method: 'get'
  })
}

export function updateCandidate(data) {
  return request({
    url: '/evaluation/candidate',
    method: 'put',
    data
  })
}

export function delCandidate(candidateId) {
  return request({
    url: '/evaluation/candidate/' + candidateId,
    method: 'delete'
  })
}

export function clearCandidate(activityId) {
  return request({
    url: '/evaluation/candidate/clear/' + activityId,
    method: 'delete'
  })
}

export function selectCandidateFromPool(activityId, data) {
  return request({
    url: '/evaluation/candidate/activity/' + activityId + '/select',
    method: 'post',
    data
  })
}

export function copyCandidateFromActivity(activityId, sourceActivityId, data) {
  return request({
    url: '/evaluation/candidate/activity/' + activityId + '/copy/' + sourceActivityId,
    method: 'post',
    data
  })
}
