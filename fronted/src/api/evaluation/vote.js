import request from '@/utils/request'

const publicHeaders = { isToken: false, isPublic: true }

export function getVoteEntry(token) {
  return request({
    url: '/evaluation/vote/entry/' + token,
    method: 'get',
    headers: publicHeaders
  })
}

export function confirmVoteIdentity(token, data) {
  return request({
    url: '/evaluation/vote/entry/' + token + '/confirm',
    method: 'post',
    data,
    headers: publicHeaders
  })
}

export function listVoteCandidates(token) {
  return request({
    url: '/evaluation/vote/entry/' + token + '/candidates',
    method: 'get',
    headers: publicHeaders
  })
}

export function submitVote(token, data) {
  return request({
    url: '/evaluation/vote/entry/' + token + '/submit',
    method: 'post',
    data,
    headers: publicHeaders
  })
}

export function getVoteResult(token) {
  return request({
    url: '/evaluation/vote/entry/' + token + '/result',
    method: 'get',
    headers: publicHeaders
  })
}
