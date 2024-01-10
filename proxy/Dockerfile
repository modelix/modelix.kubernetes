FROM nginx
COPY nginx.conf /etc/nginx/nginx.conf.template
COPY run.sh /etc/nginx/run.sh
COPY index.html /usr/share/nginx/html/
COPY public/ /usr/share/nginx/html/public/

CMD ["/etc/nginx/run.sh"]