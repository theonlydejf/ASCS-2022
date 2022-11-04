def signum(num):
    if num > 0:
        return 1
    if num < 0:
        return -1
    return 0

def normalize(ang):
    return ang % 360

def cartesian2closestRaw(raw, cartesian):
    currCartesian = normalize(raw)
    delta = cartesian - currCartesian

    if abs(delta) > 180:
        delta -= 360 * signum(delta)
    
    return raw + delta