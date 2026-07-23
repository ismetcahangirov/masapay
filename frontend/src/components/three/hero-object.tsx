import { useLayoutEffect, useMemo, useRef } from 'react'
import { Canvas, useFrame, type ThreeEvent } from '@react-three/fiber'
import {
  Color,
  CylinderGeometry,
  Group,
  IcosahedronGeometry,
  InstancedMesh,
  Mesh,
  MeshBasicMaterial,
  Object3D,
  Vector3,
} from 'three'

const UP = new Vector3(0, 1, 0)
const NEAR_BLACK = new Color('#0a0a0a')
const WIRE_GREEN = new Color('#08cb00')
const CORE_GREEN = new Color('#9dff9d')
const tmpColor = new Color()
const targetColor = new Color()

const SPARK_COUNT = 6

type Edge = { a: Vector3; b: Vector3 }

const posKey = (p: Vector3) =>
  `${Math.round(p.x * 1000)},${Math.round(p.y * 1000)},${Math.round(p.z * 1000)}`

// Unique edges of an icosahedron plus, for each edge, the indices of the edges
// that share a vertex with it (its neighbours). The geometry is non-indexed, so
// everything is keyed by rounded position.
function buildGraph(radius: number, detail: number) {
  const geo = new IcosahedronGeometry(radius, detail)
  const pos = geo.attributes.position
  const seen = new Map<string, number>()
  const edges: Edge[] = []
  const v = [new Vector3(), new Vector3(), new Vector3()]

  for (let i = 0; i < pos.count; i += 3) {
    v[0].fromBufferAttribute(pos, i)
    v[1].fromBufferAttribute(pos, i + 1)
    v[2].fromBufferAttribute(pos, i + 2)
    for (const [x, y] of [
      [0, 1],
      [1, 2],
      [2, 0],
    ] as const) {
      const ka = posKey(v[x])
      const kb = posKey(v[y])
      const k = ka < kb ? `${ka}|${kb}` : `${kb}|${ka}`
      if (seen.has(k)) continue
      seen.set(k, edges.length)
      edges.push({ a: v[x].clone(), b: v[y].clone() })
    }
  }
  geo.dispose()

  // Edges incident to each vertex, then neighbours per edge.
  const incident = new Map<string, number[]>()
  edges.forEach((e, i) => {
    for (const p of [e.a, e.b]) {
      const k = posKey(p)
      const list = incident.get(k)
      if (list) list.push(i)
      else incident.set(k, [i])
    }
  })
  const neighbours = edges.map((e, i) => {
    const set = new Set<number>()
    for (const p of [e.a, e.b]) {
      for (const j of incident.get(posKey(p)) ?? []) if (j !== i) set.add(j)
    }
    return [...set]
  })

  return { edges, neighbours }
}

// Irregular on/off flicker so the current reads as arcing, not a smooth pulse.
function strobe(time: number, seed: number) {
  const s = Math.sin((Math.floor(time * 40) + seed) * 12.9898) * 43758.5453
  const r = s - Math.floor(s)
  return r > 0.32 ? 1 : 0.55
}

function Figure({ animate }: { animate: boolean }) {
  const groupRef = useRef<Group>(null)
  const meshRef = useRef<InstancedMesh>(null)
  const hitRef = useRef<InstancedMesh>(null)
  const sparkRefs = useRef<(Mesh | null)[]>([])
  const hoveredId = useRef(-1)
  const lastHovered = useRef(-2)

  const { geometry, material, hitGeometry, hitMaterial, edges, neighbours } =
    useMemo(() => {
      const { edges, neighbours } = buildGraph(1.6, 1)
      // Thin visible wires...
      const geometry = new CylinderGeometry(0.01, 0.01, 1, 6, 1, true)
      const material = new MeshBasicMaterial({
        color: 0xffffff,
        toneMapped: false,
      })
      // ...backed by a fatter, invisible proxy so thin lines are easy to hover.
      const hitGeometry = new CylinderGeometry(0.05, 0.05, 1, 6, 1, true)
      const hitMaterial = new MeshBasicMaterial({
        transparent: true,
        opacity: 0,
        depthWrite: false,
      })
      return { geometry, material, hitGeometry, hitMaterial, edges, neighbours }
    }, [])
  const count = edges.length

  // Activation level per edge (0 = calm). Recomputed when the hovered edge
  // changes: the hovered edge plus two rings of neighbours light up so the
  // whole nearby area electrifies, not a single line.
  const activation = useMemo(() => new Float32Array(count), [count])
  const activeList = useRef<number[]>([])

  function recompute(hid: number) {
    activation.fill(0)
    activeList.current = []
    if (hid < 0 || hid >= count) return
    activation[hid] = 1
    const ring1 = neighbours[hid]
    for (const n of ring1) if (activation[n] < 0.6) activation[n] = 0.6
    for (const n of ring1)
      for (const m of neighbours[n])
        if (activation[m] < 0.32) activation[m] = 0.32
    for (let i = 0; i < count; i++)
      if (activation[i] > 0) activeList.current.push(i)
  }

  useLayoutEffect(() => {
    const inst = meshRef.current
    const hit = hitRef.current
    if (!inst) return
    const dummy = new Object3D()
    const dir = new Vector3()
    edges.forEach((e, i) => {
      dir.subVectors(e.b, e.a)
      const len = dir.length()
      dummy.position.copy(e.a).add(e.b).multiplyScalar(0.5)
      dummy.quaternion.setFromUnitVectors(UP, dir.clone().normalize())
      dummy.scale.set(1, len, 1)
      dummy.updateMatrix()
      inst.setMatrixAt(i, dummy.matrix)
      inst.setColorAt(i, NEAR_BLACK)
      hit?.setMatrixAt(i, dummy.matrix)
    })
    inst.instanceMatrix.needsUpdate = true
    if (inst.instanceColor) inst.instanceColor.needsUpdate = true
    if (hit) hit.instanceMatrix.needsUpdate = true
  }, [edges])

  useFrame((state, delta) => {
    if (animate && groupRef.current) {
      groupRef.current.rotation.x += delta * 0.15
      groupRef.current.rotation.y += delta * 0.2
    }

    if (hoveredId.current !== lastHovered.current) {
      recompute(hoveredId.current)
      lastHovered.current = hoveredId.current
    }

    const t = state.clock.elapsedTime
    const inst = meshRef.current
    const colors = inst?.instanceColor
    if (inst && colors) {
      // Active edges snap on fast so the flicker is visible; calm edges decay
      // smoothly back to black.
      const activeLerp = animate ? 0.65 : 1
      const decayLerp = animate ? Math.min(1, delta * 8) : 1
      const arr = colors.array as Float32Array
      for (let i = 0; i < count; i++) {
        const level = activation[i]
        if (level > 0) {
          const flick = animate ? strobe(t, i * 3.1) : 1
          // Hovered edge burns near-white; neighbours are green.
          if (level >= 1) targetColor.copy(CORE_GREEN)
          else targetColor.copy(WIRE_GREEN)
          targetColor.multiplyScalar(level * flick * 1.7)
        } else {
          targetColor.copy(NEAR_BLACK)
        }
        tmpColor.fromArray(arr, i * 3)
        tmpColor.lerp(targetColor, level > 0 ? activeLerp : decayLerp)
        tmpColor.toArray(arr, i * 3)
      }
      colors.needsUpdate = true
    }

    // Bright sparks flowing along the electrified edges = current arcing across
    // the cluster.
    const list = activeList.current
    for (let s = 0; s < SPARK_COUNT; s++) {
      const spark = sparkRefs.current[s]
      if (!spark) continue
      if (list.length === 0 || !animate) {
        spark.visible = false
        continue
      }
      const e = edges[list[s % list.length]]
      const u = (t * 1.4 + s * 0.37) % 1
      spark.position.lerpVectors(e.a, e.b, u)
      spark.visible = true
    }
  })

  return (
    <group ref={groupRef}>
      <instancedMesh ref={meshRef} args={[geometry, material, count]} />
      <instancedMesh
        ref={hitRef}
        args={[hitGeometry, hitMaterial, count]}
        onPointerMove={(event: ThreeEvent<PointerEvent>) => {
          event.stopPropagation()
          hoveredId.current = event.instanceId ?? -1
        }}
        onPointerOut={() => {
          hoveredId.current = -1
        }}
      />
      {Array.from({ length: SPARK_COUNT }, (_, s) => (
        <mesh
          key={s}
          ref={(el) => {
            sparkRefs.current[s] = el
          }}
          visible={false}
        >
          <sphereGeometry args={[0.035, 12, 12]} />
          <meshBasicMaterial color="#c9ffcf" toneMapped={false} />
        </mesh>
      ))}
    </group>
  )
}

// Default export so it can be React.lazy-loaded — this pulls three.js into its
// own chunk, out of the initial bundle.
export default function HeroObject({ animate = true }: { animate?: boolean }) {
  return (
    <Canvas
      camera={{ position: [0, 0, 5], fov: 40 }}
      dpr={[1, 1.5]}
      gl={{ antialias: true, alpha: true }}
      style={{ background: 'transparent' }}
    >
      <Figure animate={animate} />
    </Canvas>
  )
}
