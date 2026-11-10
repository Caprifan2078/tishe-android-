const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const html = fs.readFileSync(process.argv[2] || 'index.html','utf8');
const data = html.match(/<script type="application\/json" id="tishe-data">([\s\S]*?)<\/script>/)?.[1];
assert(data, 'Question bank missing');
const marker = "<script>(()=>{'use strict';";
const source = html.split(marker)[1]?.split('</script>')[0]?.replace(/\}\)\(\);\s*$/, '');
assert(source && source.includes('function singleReport('), 'App logic missing');
const app = {innerHTML:''};
const store = new Map();
const doc = {
 getElementById: id => id==='tishe-data' ? {textContent:data} : null,
 querySelector: id => id==='#app' ? app : id==='#toasts' ? {append:()=>{}} : null,
 querySelectorAll:()=>[], addEventListener:()=>{},documentElement:{dataset:{}},
 createElement:()=>({className:'',textContent:'',remove(){}})
};
const localStorage = {
 setItem:(k,v)=>store.set(k,v),getItem:k=>store.get(k)||null,removeItem:k=>store.delete(k)
};
const tests = "\nasync function check(){\n const routeSizes=Object.fromEntries(Object.entries(DATA.routes).map(([level,qs])=>[level,qs.length]));\n assert.deepEqual(routeSizes, {'40':40,'150':150,'300':300,'500':500});\n assert.equal(DATA.modules.length,48);\n session='demo';state=initial();\n let verified=0;\n for(const [level,ids] of Object.entries(DATA.routes)){\n   state.answers={};\n   ids.forEach((id,i)=>state.answers[id]=i%5);\n   ui.level=level;ui.idx=ids.length-1;ui.route='journey';\n   const report=sentenceReport(level);\n   assert(report.head&&report.because&&report.step&&report.ready,'Missing long result '+level);\n   moveQuestion(1);\n   assert.equal(ui.route,'report','No navigation to long result '+level);\n   assert(app.innerHTML.includes('Итог теста')&&app.innerHTML.includes('Что можно сделать дальше'),'Missing rendered long result '+level);\n   verified++;\n }\n for(const module of DATA.modules){\n   state.answers={};\n   module.questions.forEach((q,i)=>state.answers[q.id]=i%(module.id==='phq4'?4:5));\n   const report=singleReport(module);\n   assert(report.head&&report.because&&report.step,'Missing conclusion '+module.id);\n   assert(!/не заверш|ещё не пройден|мало ответов/i.test(report.head),'False unfinished state '+module.id);\n   ui.module=module.id;ui.idx=module.questions.length-1;ui.route='module';ui.phqApproved=true;\n   moveQuestion(1);\n   assert.equal(ui.route,'module-report','No report destination '+module.id);\n   assert(app.innerHTML.includes('Итог теста')&&app.innerHTML.includes('Все ответы по этой теме'),'No rendered module result '+module.id);\n   verified++;\n }\n ui.module='phq4';ui.route='module';ui.phqApproved=false;\n assert(questionPage('module').includes('Мне 18 или больше'),'PHQ-4 adult gate missing');\n state.answers={};session='demo';\n assert.equal(await setAnswer('anxiety.1',4),true);\n assert.equal(state.answers['anxiety.1'],4);\n state.answers={};session='vault';key={};salt=new Uint8Array(16);\n assert.equal(await setAnswer('anxiety.1',4),false,'Failed encrypted save incorrectly acknowledged');\n assert(!Object.prototype.hasOwnProperty.call(state.answers,'anxiety.1'),'Failed answer not rolled back');\n assert.equal(verified,52);\n console.log('PASS '+verified+' actual report destinations (48 modules + 4 long routes), adult gate and failed-save rollback');\n}\ncheck().catch(e=>{console.error(e);process.exitCode=1;});\n";
const context = {
 app,assert,process,document:doc,localStorage,console,Date,Map,Set,Math,Number,Object,
 String,Promise,JSON,Array,Error,Reflect,Uint8Array,TextEncoder,TextDecoder,URL,Blob,
 crypto:{getRandomValues:()=>{throw Error('Simulated encryption failure')}},
 setTimeout:()=>0,clearTimeout:()=>{},
 location:{search:'?tishe_qa=1'},
 window:{structuredClone,scrollTo:()=>{},addEventListener:()=>{},location:{search:'?tishe_qa=1'}}
};
vm.runInNewContext("(()=>{'use strict';"+source.replace(/\binit\(\);\s*$/,'')+tests+'})();',context,{timeout:20000});
