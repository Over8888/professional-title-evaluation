const VERSION = 6
const SIZE = 21 + (VERSION - 1) * 4
const DATA_CODEWORDS = 136
const BLOCK_COUNT = 2
const DATA_CODEWORDS_PER_BLOCK = 68
const EC_CODEWORDS_PER_BLOCK = 18
const MAX_BYTE_LENGTH = 134
const FORMAT_XOR = 0x5412
const FORMAT_POLY = 0x537

const EXP = new Array(512)
const LOG = new Array(256)

let value = 1
for (let i = 0; i < 255; i++) {
  EXP[i] = value
  LOG[value] = i
  value <<= 1
  if (value & 0x100) {
    value ^= 0x11d
  }
}
for (let i = 255; i < EXP.length; i++) {
  EXP[i] = EXP[i - 255]
}

export function createQrCodeDataUrl(text, options = {}) {
  const modules = createQrModules(text)
  const quiet = options.quietZone ?? 4
  const imageSize = SIZE + quiet * 2
  let path = ''

  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      if (modules[y][x]) {
        path += `M${x + quiet},${y + quiet}h1v1h-1z`
      }
    }
  }

  const svg = [
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${imageSize} ${imageSize}" shape-rendering="crispEdges">`,
    `<rect width="100%" height="100%" fill="#fff"/>`,
    `<path d="${path}" fill="#111827"/>`,
    '</svg>'
  ].join('')

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

export function copyQrCodeImage(dataUrl, size = 512) {
  if (!dataUrl) {
    return Promise.reject(new Error('QR code is empty'))
  }
  if (!navigator.clipboard || typeof navigator.clipboard.write !== 'function' || typeof ClipboardItem === 'undefined') {
    return Promise.reject(new Error('Image clipboard is not supported'))
  }

  return dataUrlToPngBlob(dataUrl, size).then(blob => {
    return navigator.clipboard.write([
      new ClipboardItem({ 'image/png': blob })
    ])
  })
}

function dataUrlToPngBlob(dataUrl, size) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const context = canvas.getContext('2d')
      if (!context) {
        reject(new Error('Canvas is not supported'))
        return
      }
      context.fillStyle = '#fff'
      context.fillRect(0, 0, size, size)
      context.drawImage(image, 0, 0, size, size)
      canvas.toBlob(blob => {
        blob ? resolve(blob) : reject(new Error('Failed to create QR code image'))
      }, 'image/png')
    }
    image.onerror = () => reject(new Error('Failed to load QR code image'))
    image.src = dataUrl
  })
}

function createQrModules(text) {
  const dataCodewords = encodeData(text)
  const allCodewords = addErrorCorrection(dataCodewords)
  const { modules, reserved } = createBaseMatrix()
  placeDataBits(modules, reserved, codewordsToBits(allCodewords))

  let bestModules = null
  let bestMask = 0
  let bestPenalty = Infinity

  for (let mask = 0; mask < 8; mask++) {
    const masked = applyMask(modules, reserved, mask)
    drawFormatBits(masked, mask)
    const penalty = getPenaltyScore(masked)
    if (penalty < bestPenalty) {
      bestPenalty = penalty
      bestMask = mask
      bestModules = masked
    }
  }

  drawFormatBits(bestModules, bestMask)
  return bestModules
}

function encodeData(text) {
  const bytes = Array.from(new TextEncoder().encode(text))
  if (bytes.length > MAX_BYTE_LENGTH) {
    throw new Error(`QR code text is too long: ${bytes.length}/${MAX_BYTE_LENGTH} bytes`)
  }

  const bits = []
  appendBits(bits, 0b0100, 4)
  appendBits(bits, bytes.length, 8)
  bytes.forEach(byte => appendBits(bits, byte, 8))

  const capacityBits = DATA_CODEWORDS * 8
  appendBits(bits, 0, Math.min(4, capacityBits - bits.length))
  while (bits.length % 8 !== 0) {
    bits.push(0)
  }

  const data = []
  for (let i = 0; i < bits.length; i += 8) {
    data.push(bitsToByte(bits.slice(i, i + 8)))
  }
  for (let pad = 0xec; data.length < DATA_CODEWORDS; pad ^= 0xfd) {
    data.push(pad)
  }
  return data
}

function addErrorCorrection(dataCodewords) {
  const generator = reedSolomonGenerator(EC_CODEWORDS_PER_BLOCK)
  const blocks = []

  for (let block = 0; block < BLOCK_COUNT; block++) {
    const start = block * DATA_CODEWORDS_PER_BLOCK
    const data = dataCodewords.slice(start, start + DATA_CODEWORDS_PER_BLOCK)
    blocks.push({
      data,
      ecc: reedSolomonRemainder(data, generator)
    })
  }

  const result = []
  for (let i = 0; i < DATA_CODEWORDS_PER_BLOCK; i++) {
    blocks.forEach(block => result.push(block.data[i]))
  }
  for (let i = 0; i < EC_CODEWORDS_PER_BLOCK; i++) {
    blocks.forEach(block => result.push(block.ecc[i]))
  }
  return result
}

function createBaseMatrix() {
  const modules = Array.from({ length: SIZE }, () => Array(SIZE).fill(false))
  const reserved = Array.from({ length: SIZE }, () => Array(SIZE).fill(false))

  drawFinderPattern(modules, reserved, 0, 0)
  drawFinderPattern(modules, reserved, SIZE - 7, 0)
  drawFinderPattern(modules, reserved, 0, SIZE - 7)
  drawAlignmentPattern(modules, reserved, 34, 34)
  drawTimingPatterns(modules, reserved)
  reserveFormatAreas(reserved)
  modules[SIZE - 8][8] = true
  reserved[SIZE - 8][8] = true

  return { modules, reserved }
}

function drawFinderPattern(modules, reserved, left, top) {
  for (let y = -1; y <= 7; y++) {
    for (let x = -1; x <= 7; x++) {
      const xx = left + x
      const yy = top + y
      if (!isInside(xx, yy)) continue
      const isFinder = x >= 0 && x <= 6 && y >= 0 && y <= 6
      modules[yy][xx] = isFinder && (x === 0 || x === 6 || y === 0 || y === 6 || (x >= 2 && x <= 4 && y >= 2 && y <= 4))
      reserved[yy][xx] = true
    }
  }
}

function drawAlignmentPattern(modules, reserved, centerX, centerY) {
  for (let y = -2; y <= 2; y++) {
    for (let x = -2; x <= 2; x++) {
      const xx = centerX + x
      const yy = centerY + y
      modules[yy][xx] = Math.max(Math.abs(x), Math.abs(y)) !== 1
      reserved[yy][xx] = true
    }
  }
}

function drawTimingPatterns(modules, reserved) {
  for (let i = 8; i < SIZE - 8; i++) {
    const dark = i % 2 === 0
    modules[6][i] = dark
    modules[i][6] = dark
    reserved[6][i] = true
    reserved[i][6] = true
  }
}

function reserveFormatAreas(reserved) {
  for (let i = 0; i < 9; i++) {
    reserved[8][i] = true
    reserved[i][8] = true
  }
  for (let i = 0; i < 8; i++) {
    reserved[8][SIZE - 1 - i] = true
    reserved[SIZE - 1 - i][8] = true
  }
}

function placeDataBits(modules, reserved, bits) {
  let bitIndex = 0
  let upward = true

  for (let right = SIZE - 1; right >= 1; right -= 2) {
    if (right === 6) {
      right--
    }
    for (let vertical = 0; vertical < SIZE; vertical++) {
      const y = upward ? SIZE - 1 - vertical : vertical
      for (let offset = 0; offset < 2; offset++) {
        const x = right - offset
        if (!reserved[y][x]) {
          modules[y][x] = bits[bitIndex++] === 1
        }
      }
    }
    upward = !upward
  }
}

function applyMask(modules, reserved, mask) {
  const result = modules.map(row => row.slice())
  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      if (!reserved[y][x] && maskApplies(mask, x, y)) {
        result[y][x] = !result[y][x]
      }
    }
  }
  return result
}

function drawFormatBits(modules, mask) {
  const data = (0b01 << 3) | mask
  let remainder = data
  for (let i = 0; i < 10; i++) {
    remainder = (remainder << 1) ^ (((remainder >>> 9) & 1) ? FORMAT_POLY : 0)
  }
  const bits = ((data << 10) | (remainder & 0x3ff)) ^ FORMAT_XOR
  const bit = index => ((bits >>> index) & 1) === 1

  for (let i = 0; i <= 5; i++) modules[i][8] = bit(i)
  modules[7][8] = bit(6)
  modules[8][8] = bit(7)
  modules[8][7] = bit(8)
  for (let i = 9; i < 15; i++) modules[14 - i][8] = bit(i)

  for (let i = 0; i < 8; i++) modules[8][SIZE - 1 - i] = bit(i)
  for (let i = 8; i < 15; i++) modules[SIZE - 15 + i][8] = bit(i)
  modules[SIZE - 8][8] = true
}

function maskApplies(mask, x, y) {
  switch (mask) {
    case 0: return (x + y) % 2 === 0
    case 1: return y % 2 === 0
    case 2: return x % 3 === 0
    case 3: return (x + y) % 3 === 0
    case 4: return (Math.floor(y / 2) + Math.floor(x / 3)) % 2 === 0
    case 5: return ((x * y) % 2) + ((x * y) % 3) === 0
    case 6: return (((x * y) % 2) + ((x * y) % 3)) % 2 === 0
    case 7: return (((x + y) % 2) + ((x * y) % 3)) % 2 === 0
    default: return false
  }
}

function getPenaltyScore(modules) {
  let penalty = 0
  penalty += getRunPenalty(modules)
  penalty += getBlockPenalty(modules)
  penalty += getFinderPenalty(modules)
  penalty += getBalancePenalty(modules)
  return penalty
}

function getRunPenalty(modules) {
  let penalty = 0
  for (let y = 0; y < SIZE; y++) {
    penalty += lineRunPenalty(modules[y])
  }
  for (let x = 0; x < SIZE; x++) {
    penalty += lineRunPenalty(modules.map(row => row[x]))
  }
  return penalty
}

function lineRunPenalty(line) {
  let penalty = 0
  let color = line[0]
  let length = 1
  for (let i = 1; i < line.length; i++) {
    if (line[i] === color) {
      length++
    } else {
      if (length >= 5) penalty += length - 2
      color = line[i]
      length = 1
    }
  }
  if (length >= 5) penalty += length - 2
  return penalty
}

function getBlockPenalty(modules) {
  let penalty = 0
  for (let y = 0; y < SIZE - 1; y++) {
    for (let x = 0; x < SIZE - 1; x++) {
      const color = modules[y][x]
      if (modules[y][x + 1] === color && modules[y + 1][x] === color && modules[y + 1][x + 1] === color) {
        penalty += 3
      }
    }
  }
  return penalty
}

function getFinderPenalty(modules) {
  const pattern = '10111010000'
  const reverse = '00001011101'
  let penalty = 0
  for (let y = 0; y < SIZE; y++) {
    penalty += lineFinderPenalty(modules[y], pattern, reverse)
  }
  for (let x = 0; x < SIZE; x++) {
    penalty += lineFinderPenalty(modules.map(row => row[x]), pattern, reverse)
  }
  return penalty
}

function lineFinderPenalty(line, pattern, reverse) {
  let penalty = 0
  for (let i = 0; i <= line.length - 11; i++) {
    const value = line.slice(i, i + 11).map(module => module ? '1' : '0').join('')
    if (value === pattern || value === reverse) {
      penalty += 40
    }
  }
  return penalty
}

function getBalancePenalty(modules) {
  const total = SIZE * SIZE
  const dark = modules.reduce((sum, row) => sum + row.filter(Boolean).length, 0)
  return Math.floor(Math.abs(dark * 20 - total * 10) / total) * 10
}

function reedSolomonGenerator(degree) {
  let poly = [1]
  for (let i = 0; i < degree; i++) {
    const next = new Array(poly.length + 1).fill(0)
    const root = EXP[i]
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j]
      next[j + 1] ^= gfMul(poly[j], root)
    }
    poly = next
  }
  return poly
}

function reedSolomonRemainder(data, generator) {
  const result = data.concat(new Array(generator.length - 1).fill(0))
  for (let i = 0; i < data.length; i++) {
    const factor = result[i]
    if (factor === 0) continue
    for (let j = 0; j < generator.length; j++) {
      result[i + j] ^= gfMul(generator[j], factor)
    }
  }
  return result.slice(data.length)
}

function gfMul(a, b) {
  return a === 0 || b === 0 ? 0 : EXP[LOG[a] + LOG[b]]
}

function codewordsToBits(codewords) {
  const bits = []
  codewords.forEach(codeword => appendBits(bits, codeword, 8))
  return bits
}

function appendBits(bits, value, length) {
  for (let i = length - 1; i >= 0; i--) {
    bits.push((value >>> i) & 1)
  }
}

function bitsToByte(bits) {
  return bits.reduce((value, bit) => (value << 1) | bit, 0)
}

function isInside(x, y) {
  return x >= 0 && x < SIZE && y >= 0 && y < SIZE
}
