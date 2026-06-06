<template>
  <div class="login-container">
    <div class="visual-side">
      <div class="brand-logo">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        <span>OA System</span>
      </div>
      <div class="shapes-stage" ref="stageRef">
        <div class="shapes-scene" :class="{ away: passwordVisible }">
          <div class="s s1" :style="s1Style">
            <svg viewBox="0 0 120 200" class="sv"><polygon points="25,10 110,10 85,190 0,190" fill="#6C9BCF"/><circle cx="50" cy="58" r="6" fill="white" opacity=".9" ref="w1L"/><circle cx="75" cy="58" r="6" fill="white" opacity=".9" ref="w1R"/><circle cx="50" cy="58" r="3" fill="#222" ref="p1L"/><circle cx="75" cy="58" r="3" fill="#222" ref="p1R"/><path d="M42 82 Q60 76 78 82" fill="none" stroke="#4a7ab0" stroke-width="3" stroke-linecap="round" ref="m1"/></svg></div>
          <div class="s s2" :style="s2Style">
            <svg viewBox="0 0 100 160" class="sv"><rect x="5" y="20" width="90" height="120" rx="4" fill="#8DB58D"/><ellipse cx="50" cy="20" rx="45" ry="14" fill="#7AAA7A"/><ellipse cx="50" cy="140" rx="45" ry="14" fill="#6E9E6E"/><circle cx="38" cy="64" r="5.5" fill="white" opacity=".9" ref="w2L"/><circle cx="62" cy="64" r="5.5" fill="white" opacity=".9" ref="w2R"/><circle cx="38" cy="64" r="2.8" fill="#222" ref="p2L"/><circle cx="62" cy="64" r="2.8" fill="#222" ref="p2R"/><path d="M32 88 Q50 82 68 88" fill="none" stroke="#6e9e6e" stroke-width="3" stroke-linecap="round" ref="m2"/></svg></div>
          <div class="s s3" :style="s3Style">
            <svg viewBox="0 0 90 220" class="sv"><rect x="5" y="10" width="80" height="200" rx="6" fill="#D48989"/><rect x="5" y="10" width="80" height="28" rx="6" fill="#C47878"/><circle cx="32" cy="70" r="5" fill="white" opacity=".9" ref="w3L"/><circle cx="58" cy="70" r="5" fill="white" opacity=".9" ref="w3R"/><circle cx="32" cy="70" r="2.5" fill="#222" ref="p3L"/><circle cx="58" cy="70" r="2.5" fill="#222" ref="p3R"/><path d="M25 94 Q45 88 65 94" fill="none" stroke="#b47878" stroke-width="3" stroke-linecap="round" ref="m3"/></svg></div>
          <div class="s s4" :style="s4Style">
            <svg viewBox="0 0 110 130" class="sv"><polygon points="18,8 98,8 80,122 0,122" fill="#D4A060"/><circle cx="44" cy="46" r="5" fill="white" opacity=".9" ref="w4L"/><circle cx="68" cy="46" r="5" fill="white" opacity=".9" ref="w4R"/><circle cx="44" cy="46" r="2.5" fill="#222" ref="p4L"/><circle cx="68" cy="46" r="2.5" fill="#222" ref="p4R"/><path d="M36 70 Q56 64 76 70" fill="none" stroke="#b89050" stroke-width="3" stroke-linecap="round" ref="m4"/></svg></div>
          <div class="s s5" :style="s5Style">
            <svg viewBox="0 0 80 250" class="sv"><rect x="5" y="10" width="70" height="230" rx="5" fill="#9B8DC2"/><rect x="5" y="10" width="70" height="22" rx="5" fill="#8B7DB2"/><circle cx="28" cy="74" r="5" fill="white" opacity=".9" ref="w5L"/><circle cx="52" cy="74" r="5" fill="white" opacity=".9" ref="w5R"/><circle cx="28" cy="74" r="2.5" fill="#222" ref="p5L"/><circle cx="52" cy="74" r="2.5" fill="#222" ref="p5R"/><path d="M21 98 Q40 92 59 98" fill="none" stroke="#8b7db2" stroke-width="3" stroke-linecap="round" ref="m5"/></svg></div>
        </div>
      </div>
      <div class="footer-text">&#x2728; 高效协同 · 智慧办公</div>
    </div>
    <div class="form-side">
      <div class="form-content">
        <h2>欢迎回来</h2>
        <p class="subtitle">请登录您的账号</p>
        <el-form ref="formRef" :model="form" :rules="rules">
          <div class="input-group">
            <label>用户名</label>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
          </div>
          <div class="input-group">
            <label>密码</label>
            <div class="pw-wrap">
              <el-input
                v-model="form.password"
                :type="passwordVisible ? 'text' : 'password'"
                placeholder="请输入密码"
                size="large"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="eye"
                :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                @click="passwordVisible = !passwordVisible"
              >
                <svg v-if="!passwordVisible" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              </button>
            </div>
          </div>
          <div class="options-row">
            <el-checkbox v-model="form.remember">记住账号</el-checkbox>
          </div>
          <button type="button" class="btn-primary" :disabled="loading" @click="handleLogin">
            {{ loading ? "登录中..." : "登录" }}
          </button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { useUserStore } from "@/store/user"
import { authApi } from "@/api/auth"
import type { FormInstance, FormRules } from "element-plus"

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const passwordVisible = ref(false)
const form = reactive({ username: "", password: "", remember: false })
const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 4, message: "密码至少4位", trigger: "blur" },
  ],
}

const stageRef=ref<HTMLDivElement>()
let cx=0,cy=0 // stage center
let mx=0,my=0 // mouse pos
let dist=0 // mouse distance from center
let rf=0

// Refs: pupils, whites, mouths
const p1L=ref<SVGCircleElement>(),p1R=ref<SVGCircleElement>(),p2L=ref<SVGCircleElement>(),p2R=ref<SVGCircleElement>()
const p3L=ref<SVGCircleElement>(),p3R=ref<SVGCircleElement>(),p4L=ref<SVGCircleElement>(),p4R=ref<SVGCircleElement>()
const p5L=ref<SVGCircleElement>(),p5R=ref<SVGCircleElement>()
const w1L=ref<SVGCircleElement>(),w1R=ref<SVGCircleElement>(),w2L=ref<SVGCircleElement>(),w2R=ref<SVGCircleElement>()
const w3L=ref<SVGCircleElement>(),w3R=ref<SVGCircleElement>(),w4L=ref<SVGCircleElement>(),w4R=ref<SVGCircleElement>()
const w5L=ref<SVGCircleElement>(),w5R=ref<SVGCircleElement>()
const m1=ref<SVGPathElement>(),m2=ref<SVGPathElement>(),m3=ref<SVGPathElement>(),m4=ref<SVGPathElement>(),m5=ref<SVGPathElement>()

// Base positions for eyes and mouths
interface Eye{el:SVGCircleElement;cx:number;cy:number;white:SVGCircleElement;wcx:number;wcy:number}
const eyes:Eye[]=[]
interface Mouth{el:SVGPathElement;midX:number;midY:number;span:number}
const mouths:Mouth[]=[]

// Computed scaleY for each shape based on mouse distance
const baseScale=computed(()=>{
  const s=passwordVisible.value?0:1+Math.min(dist/600,0.2)
  return {s1:s*1.02,s2:s*0.98,s3:s*1.05,s4:s*0.95,s5:s*1.08}
})
const s1Style=computed(()=>({transform:`scaleY(${baseScale.value.s1})`,transformOrigin:"bottom center",transition:"transform .03s"}))
const s2Style=computed(()=>({transform:`scaleY(${baseScale.value.s2})`,transformOrigin:"bottom center",transition:"transform .03s"}))
const s3Style=computed(()=>({transform:`scaleY(${baseScale.value.s3})`,transformOrigin:"bottom center",transition:"transform .03s"}))
const s4Style=computed(()=>({transform:`scaleY(${baseScale.value.s4})`,transformOrigin:"bottom center",transition:"transform .03s"}))
const s5Style=computed(()=>({transform:`scaleY(${baseScale.value.s5})`,transformOrigin:"bottom center",transition:"transform .03s"}))

function collect(){
  const pairs=[{p:p1L,w:w1L},{p:p1R,w:w1R},{p:p2L,w:w2L},{p:p2R,w:w2R},{p:p3L,w:w3L},{p:p3R,w:w3R},{p:p4L,w:w4L},{p:p4R,w:w4R},{p:p5L,w:w5L},{p:p5R,w:w5R}]
  for(const pr of pairs){
    if(pr.p.value&&pr.w.value){
      eyes.push({el:pr.p.value,cx:parseFloat(pr.p.value.getAttribute("cx")||"0"),cy:parseFloat(pr.p.value.getAttribute("cy")||"0"),white:pr.w.value,wcx:parseFloat(pr.w.value.getAttribute("cx")||"0"),wcy:parseFloat(pr.w.value.getAttribute("cy")||"0")})
    }
  }
  const mpairs=[{el:m1,dx:42,dy:82,sp:36},{el:m2,dx:32,dy:88,sp:36},{el:m3,dx:25,dy:94,sp:40},{el:m4,dx:36,dy:70,sp:40},{el:m5,dx:21,dy:98,sp:38}]
  for(const m of mpairs){
    if(m.el.value)mouths.push({el:m.el.value,midX:m.dx,midY:m.dy,span:m.sp})
  }
}

function onMove(e:MouseEvent){mx=e.clientX;my=e.clientY;if(stageRef.value){const r=stageRef.value.getBoundingClientRect();cx=r.left+r.width/2;cy=r.top+r.height/2;dist=Math.sqrt((mx-cx)**2+(my-cy)**2)}}

function anim(){
  // Update eyes (whites + pupils follow mouse)
  for(const e of eyes){
    if(passwordVisible.value){e.el.setAttribute("cx",`${e.cx-4}`);e.white.setAttribute("cx",`${e.wcx-4}`);e.white.setAttribute("cy",`${e.wcy}`);continue}
    const r=e.el.getBoundingClientRect(),ex=r.left+r.width/2,ey=r.top+r.height/2
    const dx=mx-ex,dy=my-ey,ds=Math.sqrt(dx*dx+dy*dy),cl=Math.min(ds/120,1)
    const ox=(dx/(ds||1))*3.5*cl,oy=(dy/(ds||1))*2*cl
    e.el.setAttribute("cx",`${e.cx+ox}`);e.el.setAttribute("cy",`${e.cy+oy}`)
    e.white.setAttribute("cx",`${e.wcx+ox}`);e.white.setAttribute("cy",`${e.wcy+oy}`)
  }
  // Update mouths (curve follows mouse Y)
  for(const m of mouths){
    if(passwordVisible.value){m.el.setAttribute("d",`M${m.midX-m.span/2} ${m.midY} Q${m.midX} ${m.midY-6} ${m.midX+m.span/2} ${m.midY}`);continue}
    const dy=my-(cy||400),cl=Math.max(-1,Math.min(1,dy/200))
    const cpY=m.midY-cl*8
    m.el.setAttribute("d",`M${m.midX-m.span/2} ${m.midY} Q${m.midX} ${cpY} ${m.midX+m.span/2} ${m.midY}`)
  }
  rf=requestAnimationFrame(anim)
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await authApi.login({
      username: form.username,
      password: form.password,
    })
    userStore.setLoginResult(res)
    if (form.remember) {
      localStorage.setItem("remembered_username", form.username)
    } else {
      localStorage.removeItem("remembered_username")
    }
    ElMessage.success("登录成功")
    router.push("/welcome")
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : "登录失败，请检查用户名和密码"
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
onMounted(()=>{const s=localStorage.getItem("remembered_username");if(s){form.username=s;form.remember=true}window.addEventListener("mousemove",onMove);setTimeout(()=>{collect();anim()},200)})
onUnmounted(()=>{window.removeEventListener("mousemove",onMove);cancelAnimationFrame(rf)})
</script>

<style scoped>
*{margin:0;padding:0;box-sizing:border-box}
.login-container{display:flex;width:100%;height:100vh;background:#fff}
.visual-side{width:45%;background:#7e8b9c;color:white;display:flex;flex-direction:column;justify-content:space-between;padding:40px 40px 0;overflow:hidden}
.brand-logo{font-weight:700;font-size:18px;display:flex;align-items:center;gap:10px;z-index:10}
.shapes-stage{flex:1;position:relative;overflow:hidden;margin:0 -40px}
.shapes-scene{position:absolute;bottom:-30px;left:50%;transform:translateX(-50%);width:500px;height:450px;transition:transform .4s ease-out}
.shapes-scene.away{transform:translateX(-160px)}
.s{position:absolute;bottom:0;transform-origin:bottom center}
.sv{width:100%;height:100%;filter:drop-shadow(0 4px 12px rgba(0,0,0,.12));display:block}
.s1{width:160px;height:240px;left:130px;z-index:25}
.s2{width:130px;height:180px;left:260px;z-index:20}
.s3{width:120px;height:270px;left:50px;z-index:30}
.s4{width:140px;height:150px;left:200px;z-index:15}
.s5{width:100px;height:300px;left:340px;z-index:35}
.s1{animation:b1 3.2s ease-in-out infinite}
.s2{animation:b2 3.8s ease-in-out infinite .5s}
.s3{animation:b3 2.8s ease-in-out infinite .2s}
.s4{animation:b4 4s ease-in-out infinite .8s}
.s5{animation:b5 3.5s ease-in-out infinite .3s}
@keyframes b1{0%,100%{transform:translateY(0)}50%{transform:translateY(-6px)}}
@keyframes b2{0%,100%{transform:translateY(0)}50%{transform:translateY(-4px)}}
@keyframes b3{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}
@keyframes b4{0%,100%{transform:translateY(0)}50%{transform:translateY(-3px)}}
@keyframes b5{0%,100%{transform:translateY(0)}50%{transform:translateY(-7px)}}
.away .s1{transform:translateX(-70px) rotate(-8deg)!important;animation:none}
.away .s2{transform:translateX(-90px) rotate(6deg)!important;animation:none}
.away .s3{transform:translateX(-60px) rotate(-11deg)!important;animation:none}
.away .s4{transform:translateX(-110px) rotate(10deg)!important;animation:none}
.away .s5{transform:translateX(-45px) rotate(-6deg)!important;animation:none}
.footer-text{font-size:13px;opacity:.7;text-align:center;z-index:10;padding-bottom:24px}
.form-side{flex:1;display:flex;justify-content:center;align-items:center;background:white}
.form-content{width:100%;max-width:380px;padding:20px}
.form-content h2{font-size:28px;font-weight:700;color:#111827;margin-bottom:6px}
.subtitle{color:#6b7280;margin-bottom:30px;font-size:14px}
.input-group{margin-bottom:22px}
.input-group label{display:block;font-weight:600;font-size:14px;margin-bottom:8px;color:#111827}
:deep(.el-input__wrapper){border-radius:9999px!important;width:100%;padding:4px 20px;border:1px solid #d1d5db;background:white;box-shadow:none;transition:.2s;height:46px}
:deep(.el-input__wrapper:hover),:deep(.el-input__wrapper.is-focus){border-color:#4f46e5;box-shadow:0 0 0 2px rgba(79,70,229,.1)}
:deep(.el-input__inner){font-size:14px;height:38px}
.pw-wrap{position:relative}
.eye{position:absolute;right:18px;top:50%;transform:translateY(-50%);z-index:2;cursor:pointer;display:flex;align-items:center}
.options-row{display:flex;justify-content:space-between;align-items:center;font-size:13px;margin:0 0 22px}
.options-row :deep(.el-checkbox__label){color:#374151;font-size:13px}
.forgot-link{color:#4f46e5;text-decoration:none;font-weight:500}
.btn-primary{width:100%;height:46px;border-radius:9999px;background:white;border:1px solid #d1d5db;color:#111827;font-weight:600;font-size:15px;cursor:pointer;transition:.2s;margin-bottom:14px}
.btn-primary:hover{background:#f3f4f6}
.btn-primary:disabled{opacity:.6;cursor:not-allowed}
.btn-google{width:100%;padding:12px;background:transparent;border:1px solid #d1d5db;border-radius:9999px;font-weight:500;font-size:14px;cursor:pointer;transition:.2s;color:#374151}
.btn-google:hover{background:#f9fafb}
.signup-link{text-align:center;font-size:14px;color:#6b7280;margin-top:22px}
.signup-link a{color:#4f46e5;text-decoration:none;font-weight:600}
</style>
