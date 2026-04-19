local stockKey = KEYS[1]
local limitKey = KEYS[2]
local requestKey = KEYS[3]

local nowMillis = tonumber(ARGV[1])
local startMillis = tonumber(ARGV[2])
local endMillis = tonumber(ARGV[3])
local purchaseLimit = tonumber(ARGV[4])
local requestTtlSeconds = tonumber(ARGV[5])

if nowMillis < startMillis then
    return 1
end

if nowMillis > endMillis then
    return 2
end

if redis.call('EXISTS', requestKey) == 1 then
    return 4
end

local currentPurchased = tonumber(redis.call('GET', limitKey) or '0')
if currentPurchased >= purchaseLimit then
    return 5
end

local stock = tonumber(redis.call('GET', stockKey) or '0')
if stock <= 0 then
    return 3
end

redis.call('DECR', stockKey)
redis.call('INCR', limitKey)
redis.call('SET', requestKey, '1', 'EX', requestTtlSeconds)

return 0
