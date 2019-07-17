create or replace package ifpks_http_servlet
is
g_servlet_url varchar2(255) := 'http://localhost:7001/WSCallerServlet/WSCallerServlet';
g_servlet_uid varchar2(255) := 'weblogic';
g_servlet_pwd varchar2(255) := 'Oracle123';
g_timeout_sec number := 60;

type ty_header is table of varchar2(4000) index by varchar2(255);
tb_header ty_header;
function fn_http_get(p_url in varchar2, p_header in ty_header := tb_header) return clob;
function fn_http_post(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob;
function fn_http_put(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob;
function fn_http_patch(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob;
function fn_http_delete(p_url in varchar2, p_header in ty_header := tb_header) return clob;
end;
/
create or replace package body ifpks_http_servlet
is
function fn_req_msg(p_url in varchar2,p_payload in varchar2, p_Content_Type in varchar2, p_header in ty_header,p_http_verb in varchar2) return clob
is
  l_header       varchar2(4000);
  l_header_xml   clob;
  l_tb_header    ty_header := p_header; 
  function xmlt (tag in varchar2, val in varchar2) return varchar2
  is
  begin
    return '<'||tag||'>'||val||'</'||tag||'>'||chr(10);
  end;
begin
  if p_Content_Type is not null then
    l_tb_header('Content-Type') := p_Content_Type;
  end if;
  if l_tb_header.count > 0
  then
    l_header := l_tb_header.first;
    while l_header is not null
    loop
      if lower(l_header) != 'content-length'
      then
        l_header_xml := l_header_xml||
                            xmlt('WS_HTTP_HEADER',
                                     xmlt('WS_HTTP_HEADER_NAME',l_header)||
                                     xmlt('WS_HTTP_HEADER_VALUE',l_tb_header(l_header))
                                   );
         l_header := l_tb_header.next(l_header);
       end if;
    end loop;
  end if;

  return xmlt('WS_REQUEST',
                 (
                 xmlt('WS_HTTP_VERB',p_http_verb)      ||
                 xmlt('WS_URL',p_url)                      ||  
                 xmlt('WS_HTTP_HEADERS',l_header_xml)      ||
                 xmlt('WS_PAYLOAD','<![CDATA['||p_payload||']]>')
                 ));
end;

function fn_servlet (p_url in varchar2, p_payload in clob, p_Content_Type in varchar2, p_header in ty_header,p_http_verb in varchar2)
return clob
is
  l_req              utl_http.req;
  l_res              utl_http.resp;
  l_buffer           varchar2(32767);
  l_resp_msg         clob;
  l_req_msg          clob;
begin
  l_req_msg := fn_req_msg(p_url,p_payload,p_Content_Type,p_header,p_http_verb);
  l_req := utl_http.begin_request(g_servlet_url, 'POST',' HTTP/1.1');
  utl_http.set_authentication(l_req,g_servlet_uid,g_servlet_pwd);
  utl_http.set_header(l_req, 'user-agent', 'mozilla/4.0');
  utl_http.set_header(l_req, 'Content-Type', 'text/xml');
  utl_http.set_header(l_req, 'Content-Length', length(l_req_msg));
  utl_http.set_transfer_timeout(timeout => g_timeout_sec);
  utl_http.write_text(l_req, l_req_msg);
  l_res := utl_http.get_response(l_req);
  begin
    loop
      utl_http.read_line(l_res, l_buffer);
      l_resp_msg := l_resp_msg||l_buffer;
    end loop;
  exception
    when utl_http.end_of_body
    then
      null;
  end;
  utl_http.end_response(l_res);
  return l_resp_msg;
  
end;

function fn_http_get(p_url in varchar2, p_header in ty_header := tb_header) return clob
is
begin
  return fn_servlet (p_url, null, null,p_header,'GET');
end;
function fn_http_post(p_url in varchar2, p_payload in clob,  p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
    return fn_servlet (p_url, p_payload, p_Content_Type,p_header, 'POST');
end;
function fn_http_put(p_url in varchar2, p_payload in clob,  p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
    return fn_servlet (p_url, p_payload, p_Content_Type,p_header, 'PUT');
end;
function fn_http_patch(p_url in varchar2, p_payload in clob,  p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
    return fn_servlet (p_url, p_payload, p_Content_Type,p_header, 'DELETE');
end;
function fn_http_delete (p_url in varchar2, p_header in ty_header := tb_header) return clob
is
begin
    return fn_servlet (p_url, null, null,p_header, 'PATCH');
end;
end;
/
