/**
* @function getDate 获取日期
* @param  {timestamp} timestamp {时间戳}
* @return {String} {2017-02-02}
*/
const getDate = (timestamp) => {
  if (timestamp && typeof timestamp !== 'number') {
    throw new Error('请输入数字类型参数!');
  }
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${year}-${month >= 10 ? month : '0' + month}-${day >= 10 ? day : '0' + day}`
}
/**
* @function getDate 获取详细时间，格式为： 年-月-日 时:分:秒
* @param  {timestamp} timestamp {时间戳}
* @return {String} {2019-10-31 16:39:00}
*/
const getDateDetail = (timestamp) => {
  if (timestamp && typeof timestamp !== 'number') {
    throw new Error('请输入数字类型参数!');
  }
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()
  return `${year}-${month >= 10 ? month : '0' + month}-${day >= 10 ? day : '0' + day} ${hour >= 10 ? hour : '0' + hour}:${minute >= 10 ? minute : '0' + minute}:${second >= 10 ? second : '0' + second}`
}
/**
 * 将00:00格式的数据转成00s
 * @param  {String} duration 00:00格式的时间
 * @return {[type]}          [description]
 */
const convertStrToInt = (duration) => {
  duration = duration || '00:00'
  const arr = duration.split(':')
  if (arr[0] === '00' && Number(arr[1]) > 0) {
    duration = "00'" + arr[1] + '"'
  } else {
    if (arr[1] === '00' && Number(arr[0] > 0)) {
      duration = arr[0] + "'" + '00"'
    } else {
      duration = arr[0] + "'" + arr[1] + '"'
    }
  }
  if (duration === '0"' || duration === "0'") {
    duration = ''
  }
  return duration
}
/**
 * 将00s转成00:00
 * @param  {[type]} timestamp [description]
 * @return {[type]}           [description]
 */
/**
* @function dateFormat 时间格式化
* @param  {timestamp} timestamp {时间戳}
* @return {String}
*/
const dateBeforeAfter = (timestamp) => {
  const now = Date.now()
  const minute = 1000 * 60
  const hour = minute * 60
  const publishTime = String(now).length === String(timestamp).length ? timestamp : timestamp * 1000
  const diff = now - publishTime
  if (diff / hour >= 1) {
    if (diff / hour >= 24) {
      return getDate(publishTime)
    } else {
      return `${parseInt(diff / hour, 10)}小时前`
    }
  } else if (diff / minute >= 1) {
    return `${parseInt(diff / minute, 10)}分钟前`
  } else {
    return '1分钟前'
  }
}
/**
 * 转换星座
 * @param  {Number} month [月份]
 * @param  {Number} day   [日期]
 * @return {[type]}       [description]
 */
const getAstro = (month, day) => {
  var s = '魔羯水瓶双鱼白羊金牛双子巨蟹狮子处女天秤天蝎射手魔羯'
  var arr = [20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22]
  return s.substr(month * 2 - (day < arr[month - 1] ? 2 : 0), 2)
}
/**
 * 获取年龄
 * @param  {[Number]} y [年]
 * @param  {[Number]} m [月]
 * @param  {[Number]} d [日]
 * @return {[type]}   [description]
 */
const getAge = (y, m, d) => {
  var today = new Date()
  var birthDate = new Date(y, m, d)
  var age = today.getFullYear() - birthDate.getFullYear()
  var month = today.getMonth() - birthDate.getMonth()
  if (month < 0 || (month === 0 && today.getDate() < birthDate.getDate())) {
    age--
  }
  return age
}

const getLastWeek = () => {
  let lastWeek = []
  for (let i = 6; i > -1; i--) {
    let time = new Date().getTime()
    time = time - (i * 24 * 3600 * 1000)
    let y = new Date(time).getFullYear()
    let m = new Date(time).getMonth() + 1
    m = m > 9 ? m : '0' + m
    let d = new Date(time).getDate()
    d = d > 9 ? d : '0' + d
    let day = y + '-' + m + '-' + d
    lastWeek.push(day)
  }
  return lastWeek
}

const getTimeList = (start, end) => {
  let n = 0
  let getDayList = []
  do {
    let d = 24 * 3600 * 1000
    var thisData = d * n + start
    getDayList.push(getDate(thisData))
    n++
  } while (thisData < end)
  return getDayList
}

export {
  getAge,
  getAstro,
  dateBeforeAfter,
  convertStrToInt,
  getDate,
  getDateDetail,
  getLastWeek,
  getTimeList
}
