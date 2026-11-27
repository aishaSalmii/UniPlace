"""M03/PERF-01: single authenticated coordinator, 100 sequential GETs.
No database generation/reset. No third-party packages. Password/cookies never saved.
"""
import argparse,csv,getpass,http.cookiejar,json,math,platform,re,statistics,subprocess,time,urllib.parse,urllib.request
from datetime import datetime,timezone
from html.parser import HTMLParser
from pathlib import Path

class Page(HTMLParser):
    def __init__(self):
        super().__init__();self.text=[];self.token=None;self.rows=[];self.row=None;self.cell=None;self.depth=0
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=='input' and a.get('name')=='_csrf':self.token=a.get('value')
        if tag=='table':self.depth+=1
        if self.depth and tag=='tr':self.row=[]
        if self.depth and tag=='td':self.cell=[]
    def handle_data(self,data):
        self.text.append(data)
        if self.cell is not None:self.cell.append(data)
    def handle_endtag(self,tag):
        if tag=='td' and self.cell is not None:
            if self.row is not None:self.row.append(''.join(self.cell).strip())
            self.cell=None
        if tag=='tr' and self.row is not None:
            if self.row:self.rows.append(self.row)
            self.row=None
        if tag=='table':self.depth-=1
    def info(self):
        m=re.search(r'Page\s+(\d+)\s+of\s+(\d+)\s*·\s*(\d+)\s+applications',' '.join(self.text))
        if not m:raise ValueError('Application-list pager missing (possibly login/error page)')
        return tuple(map(int,m.groups()))

def command(*args):
    try:return subprocess.check_output(args,text=True,stderr=subprocess.DEVNULL).strip()
    except Exception:return 'unavailable'
def p95(values):return sorted(values)[math.ceil(.95*len(values))-1]
def validate(html,final_url,page_number,expected):
    p=Page();p.feed(html)
    actual,pages,total=p.info()
    if '/coordinator/applications' not in urllib.parse.urlsplit(final_url).path:raise ValueError('Unexpected redirect')
    if total!=expected or actual!=page_number+1 or pages!=math.ceil(expected/20):raise ValueError('Unexpected dataset or page number')
    needed=min(20,expected-page_number*20)
    if len(p.rows)!=needed or any(len(r)!=7 for r in p.rows):raise ValueError('Incorrect row count or table shape')
    ids=[r[0] for r in p.rows]
    if len(set(ids))!=len(ids):raise ValueError('Repeated application ID within a page')
    return len(ids)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--base',default='http://127.0.0.1:8080');ap.add_argument('--expected-total',type=int,default=1117);args=ap.parse_args()
    u=urllib.parse.urlsplit(args.base)
    if u.hostname not in ['127.0.0.1','localhost'] or u.scheme!='http':raise SystemExit('This runner is restricted to the local test Portal.')
    if args.expected_total<1001:raise SystemExit('Dataset must include seed plus 1,000 extra applications.')
    base=args.base.rstrip('/');opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),urllib.request.ProxyHandler({}))
    def fetch(path,data=None):
        with opener.open(base+path,data=data,timeout=15) as r:return r.read().decode('utf-8'),r.geturl(),r.status
    try:
        html,_,_=fetch('/login');p=Page();p.feed(html)
        if not p.token:raise ValueError('Login CSRF token missing')
        password=getpass.getpass('Coordinator password (hidden): ')
        payload=urllib.parse.urlencode({'username':'coordinator','password':password,'_csrf':p.token}).encode()
        fetch('/login',payload);del password,payload
        html,url,_=fetch('/coordinator/applications?page=0');validate(html,url,0,args.expected_total)
    except Exception as e:raise SystemExit('Login/dataset check failed: '+type(e).__name__+'. Keep website running and verify login and total; no measurements reported.')
    last=math.ceil(args.expected_total/20)-1
    pages=[0,1,last//2,last] # predeclared mixed pagination workload
    out=Path('evidence')/('m03-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'));out.mkdir(parents=True)
    env={'time_utc':datetime.now(timezone.utc).isoformat(),'platform':platform.platform(),'cpu':command('sysctl','-n','machdep.cpu.brand_string'),'physical_cores':command('sysctl','-n','hw.physicalcpu'),'logical_cores':command('sysctl','-n','hw.logicalcpu'),'memory_bytes':command('sysctl','-n','hw.memsize'),'git_commit':command('git','rev-parse','HEAD'),'git_tracked_status':command('git','status','--short','--untracked-files=no'),'java_version':command('java','--version'),'mode':'one authenticated coordinator, 100 sequential HTTP GETs; loopback client and server on same host','expected_total':args.expected_total,'page_size':20,'pages_zero_based':pages,'warmups':8,'measured_requests':100,'timeout_seconds':15,'threshold_ms':2000,'percentile_method':'nearest rank: sorted[ceil(0.95*N)-1]','timing_scope':'request start through complete response-body read; excludes HTML parsing, login, static assets and browser rendering','reference_hardware':'requirement reference: 4 cores / 8 GB; this host is not constrained to that reference','scope_limitations':'No filters, student/recruiter ownership, concurrency or 50-user workload tested. This is local pagination timing, not complete PERF-01 compliance.'}
    (out/'environment-and-plan.json').write_text(json.dumps(env,indent=2))
    for i in range(8):
        n=pages[i%4];h,u,_=fetch('/coordinator/applications?page='+str(n));validate(h,u,n,args.expected_total)
    rows=[]
    with (out/'requests.csv').open('w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=['sample','page_zero_based','elapsed_ms','http_status','valid_content','row_count','error_type']);writer.writeheader()
        for i in range(100):
            n=pages[i%4];record={'sample':i+1,'page_zero_based':n,'http_status':None,'valid_content':False,'row_count':None,'error_type':''}
            start=time.perf_counter()
            try:
                html,url,status=fetch('/coordinator/applications?page='+str(n));elapsed=(time.perf_counter()-start)*1000
                record.update(elapsed_ms=round(elapsed,6),http_status=status)
                record['row_count']=validate(html,url,n,args.expected_total);record['valid_content']=status==200
            except Exception as e:
                record.setdefault('elapsed_ms',round((time.perf_counter()-start)*1000,6));record['error_type']=type(e).__name__
            rows.append(record);writer.writerow(record);f.flush()
    timings=[r['elapsed_ms'] for r in rows];errors=sum(not r['valid_content'] for r in rows)
    result={'samples':100,'invalid_or_error_responses':errors,'mean_ms':statistics.mean(timings),'median_ms':statistics.median(timings),'p95_ms':p95(timings),'max_ms':max(timings),'local_measured_workload_verdict':'PASS' if errors==0 and p95(timings)<=2000 else 'FAIL','requirement_verdict':'PARTIAL EVIDENCE ONLY: actual hardware differs from reference; filters and ownership not assessed','per_page':{str(n):{'samples':25,'p95_ms':p95([r['elapsed_ms'] for r in rows if r['page_zero_based']==n])} for n in pages}}
    (out/'summary.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));print('Evidence saved:',out)
if __name__=='__main__':main()
