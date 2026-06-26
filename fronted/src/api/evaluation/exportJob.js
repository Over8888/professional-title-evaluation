import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

export function listExportJob(query) {
  return request({
    url: '/evaluation/export-job/list',
    method: 'get',
    params: query
  })
}

export function getExportJob(id) {
  return request({
    url: '/evaluation/export-job/' + parseStrEmpty(id),
    method: 'get'
  })
}

export function delExportJob(ids) {
  return request({
    url: '/evaluation/export-job/' + ids,
    method: 'delete'
  })
}
