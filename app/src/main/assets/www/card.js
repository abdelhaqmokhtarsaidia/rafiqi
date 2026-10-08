/* رسم بطاقة الصلاة القادمة (بنفسجي وأسود) — تُستعمل من الصفحة ومن الـ service worker */
function drawCard(cv,nm,hm,dt,hj,fr){const W=720,H=360;cv.width=W;cv.height=H;const x=cv.getContext('2d');
let g=x.createLinearGradient(0,0,W,H);g.addColorStop(0,'#07040d');g.addColorStop(.55,'#1c0d36');g.addColorStop(1,'#4a2390');x.fillStyle=g;x.fillRect(0,0,W,H);
let r=x.createRadialGradient(W*.82,H*.2,10,W*.82,H*.2,260);r.addColorStop(0,'rgba(167,112,255,.45)');r.addColorStop(1,'rgba(167,112,255,0)');x.fillStyle=r;x.fillRect(0,0,W,H);
x.strokeStyle='rgba(201,167,255,.10)';x.lineWidth=2;for(let i=1;i<=6;i++){x.beginPath();x.arc(fr?W-90:90,H*.22,i*46,0,7);x.stroke()}
x.fillStyle='rgba(255,255,255,.7)';for(let i=0;i<26;i++){const px=(i*137%W),py=(i*71%(H-90)),s=((i*7)%3)+1;x.globalAlpha=.15+((i*13)%5)/12;x.fillRect(px,py,s,s)}x.globalAlpha=1;
const f='"Segoe UI",Tahoma,"Noto Naskh Arabic",Arial,sans-serif',R=fr?60:W-60,A=fr?'left':'right';x.textBaseline='middle';x.direction=fr?'ltr':'rtl';x.textAlign=A;
x.fillStyle='#c9a7ff';x.font='600 27px '+f;x.fillText(fr?'PROCHAINE PRIÈRE':'الصلاة القادمة',R,52);
x.fillStyle='#fff';x.font='700 96px '+f;x.fillText(nm,R,142);
x.direction='ltr';x.fillStyle='#e4d2ff';x.font='700 78px '+f;x.fillText(hm,R,232);
x.fillStyle='rgba(201,167,255,.35)';x.fillRect(60,282,W-120,1.5);
x.direction=fr?'ltr':'rtl';x.fillStyle='#fff';x.font='500 26px '+f;x.fillText(dt,R,312);
x.fillStyle='rgba(228,210,255,.8)';x.font='500 22px '+f;x.fillText(hj,R,340)}
