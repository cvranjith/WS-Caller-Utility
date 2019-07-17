create or replace package ifpks_http_mdb
is
g_mdb_req_q   varchar2(255) := 'Q';
g_mdb_reply_q varchar2(255) := 'RQ';
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
create or replace package body ifpks_http_mdb
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

function fn_mdb(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2, p_header in ty_header, p_http_verb in varchar2)
return clob
is
l_enqueue_options     dbms_aq.enqueue_options_t;
l_req_msg_prop        dbms_aq.message_properties_t;
l_req_msg_id          raw(16);
l_req_msg             sys.aq$_jms_text_message;
l_dequeue_options     dbms_aq.dequeue_options_t;
l_resp_msg_prop       dbms_aq.message_properties_t;
l_resp_msg            sys.aq$_jms_text_message;
l_resp_msg_id         raw(16);
l_resp_msg_txt        clob;

no_messages              exception;
pragma exception_init    (no_messages, -25228);
pragma autonomous_transaction;
begin
  l_req_msg := sys.aq$_jms_text_message.construct;
  l_req_msg.set_text(fn_req_msg(p_url,p_payload,p_Content_Type,p_header,p_http_verb));
  dbms_aq.enqueue
       (
       queue_name => g_mdb_req_q,
       enqueue_options => l_enqueue_options,
       message_properties => l_req_msg_prop,
       payload => l_req_msg,
       msgid => l_req_msg_id
       );
  commit;
  l_dequeue_options.wait := g_timeout_sec;
  l_dequeue_options.correlation := 'ID:'||l_req_msg_id;
  l_dequeue_options.navigation := dbms_aq.first_message;
  begin
      dbms_aq.dequeue
           (
           queue_name => g_mdb_reply_q,
           dequeue_options => l_dequeue_options,
           message_properties => l_resp_msg_prop,
           payload => l_resp_msg,
           msgid => l_resp_msg_id
           );
      commit;
      l_resp_msg.get_text(l_resp_msg_txt);
  exception when no_messages
  then
       l_resp_msg_txt := '{"error" : "Timed Out. No Response recieved in '||g_mdb_reply_q||' after waiting for '||g_timeout_sec||' seconds"}';
  end;
  return l_resp_msg_txt;
end;


function fn_http_get(p_url in varchar2, p_header in ty_header := tb_header) return clob
is
begin
  return fn_mdb(p_url, null, null, p_header,'GET');
end;
function fn_http_post(p_url in varchar2, p_payload in clob,  p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
  return fn_mdb(p_url, p_payload,p_Content_Type, p_header,'POST');
end;

function fn_http_put(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
  return fn_mdb(p_url, p_payload,p_Content_Type, p_header,'PUT');
end;
function fn_http_patch(p_url in varchar2, p_payload in clob, p_Content_Type in varchar2 := null, p_header in ty_header := tb_header) return clob
is
begin
  return fn_mdb(p_url, p_payload,p_Content_Type, p_header,'PATCH');
end;
function fn_http_delete(p_url in varchar2, p_header in ty_header := tb_header) return clob
is
begin
  return fn_mdb(p_url, null,null, p_header,'DELETE');
end;
end;
/
