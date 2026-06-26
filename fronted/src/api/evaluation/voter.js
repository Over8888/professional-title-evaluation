import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

export function listVoter(query) {
  return request({
    url: '/evaluation/voter/list',
    method: 'get',
    params: query
  })
}

export function listVoterOptions() {
  return request({
    url: '/evaluation/voter/options',
    method: 'get'
  })
}

export function getVoter(voterId) {
  return request({
    url: '/evaluation/voter/' + parseStrEmpty(voterId),
    method: 'get'
  })
}

export function updateVoter(data) {
  return request({
    url: '/evaluation/voter',
    method: 'put',
    data
  })
}

export function delVoter(voterId) {
  return request({
    url: '/evaluation/voter/' + voterId,
    method: 'delete'
  })
}

export function clearVoter(activityId) {
  return request({
    url: '/evaluation/voter/clear/' + activityId,
    method: 'delete'
  })
}

export function generateVoterToken(voterId) {
  return request({
    url: '/evaluation/voter/' + voterId + '/token',
    method: 'put'
  })
}

export function getVoterProgress(activityId) {
  return request({
    url: '/evaluation/voter/progress/' + activityId,
    method: 'get'
  })
}

export function getActivityVoteLinks(activityId) {
  return request({
    url: '/evaluation/voter/activity/' + activityId + '/links',
    method: 'get'
  })
}

export function selectVoterFromPool(activityId, data) {
  return request({
    url: '/evaluation/voter/activity/' + activityId + '/select',
    method: 'post',
    data
  })
}

export function copyVoterFromActivity(activityId, sourceActivityId, data) {
  return request({
    url: '/evaluation/voter/activity/' + activityId + '/copy/' + sourceActivityId,
    method: 'post',
    data
  })
}
