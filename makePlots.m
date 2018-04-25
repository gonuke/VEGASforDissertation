load annualReports;
n_columns=length(annualReports(1,:));
n_non_rx=19;
n_rx=(n_columns-n_non_rx)/2;
rx_names=textread('rxNames','%s',n_rx);
year=annualReports(:,1);
rx=zeros(length(annualReports(:,1)),n_rx);
rx_sf=zeros(length(annualReports(:,1)),n_rx);
cp=zeros(3,n_rx);
red=0;
green=0;
blue=0;
rgb=0;
delta_c=0.4;
loc_0=round(3*length(year)/8);
loc_1=round(length(year)/2);
loc_2=round(5*length(year)/8);
for (i=2:(n_rx+1))
    rx(:,i-1)=annualReports(:,i)/1.e6;
    if(rgb==0)
        red = red + delta_c;
        if(red > 1) 
            red = red - 1.;
        end
    end
    if(rgb==1)
        green = green + delta_c;
        if(green > 1) 
            green = green - 1.;
        end 
    end
    if(rgb==2)
        blue = blue + delta_c;
        if(blue > 1) 
            blue = blue - 1.;
        end 
    end
    rgb = rgb + 1;
    if(rgb > 2) 
        rgb=0;
    end
    if(red == 0 && green == 0 && blue == 0)
        red = 0.33;
        blue = 0.33;
    end
    cp(:,i-1)=[red green blue]';
end
totcap=annualReports(:,n_rx+2)/1.e6;
coe=annualReports(:,n_rx+3);
sf=annualReports(:,n_rx+4)/1.e3;
pu=annualReports(:,n_rx+5)/1.e3;
ma=annualReports(:,n_rx+6)/1.e3;
fc=annualReports(:,n_rx+7);
sflt=annualReports(:,n_rx+8)/1.e3;
pult=annualReports(:,n_rx+9)/1.e3;
malt=annualReports(:,n_rx+10)/1.e3;
rep0=annualReports(:,n_rx+12)/1.e3;
rep1=annualReports(:,n_rx+13)/1.e3;
rep2=annualReports(:,n_rx+14)/1.e3;
sfoop=annualReports(:,n_rx+15)/1.e3;
pu_waste=annualReports(:,n_rx+16);
ma_waste=annualReports(:,n_rx+17);
nu_use=annualReports(:,n_rx+18)/1.e9;
class_c=annualReports(:,n_rx+19)/1.e9;
for (i=1:n_rx)
    rx_sf(:,i)=annualReports(:,n_rx+19+i)/1.e3;
end
startyear=year(1)+10;
endyear=year(length(year))-10;
lwrcoe=3.94;
lwrfc=0.454;
legacysf=0.;%2.05e7;
clf reset;
subplot(2,3,1);
plot(year,totcap,'--c','LineWidth',3);
hold on;
text(year(30),totcap(40)*.98,'Total','Color','c','FontSize',16);
for (i=1:n_rx)
    if(max(rx(:,i))>1.)
        plot(year,rx(:,i),'Color',cp(:,i)','LineWidth',2);
        text(year(40),rx(40,i)*.95,rx_names(i),'Color',cp(:,i)','FontSize',12);
    end;
end;
xlabel('Year','FontSize',12);
ylabel('Generation Capacity [GWe]','FontSize',12);
xlim([startyear endyear]);
ylim([0 max(totcap)*1.1]);
set(gca,'FontSize',12);
subplot(2,3,2);
plot(year,nu_use,'Color',[.1 .2 .8],'LineWidth',2);
text(year(loc_1),nu_use(loc_1)*1.05,'U_{nat} Usage','Color',[.1 .2 .8],'FontSize',14);
hold on;
plot(year,class_c,'Color',[.7 0 .3],'LineWidth',1.5);
text(year(loc_0),class_c(loc_0)*1.05,'DU and RU in Storage','Color',[.7 .0 .3],'FontSize',14);
ylabel('Uranium Mass [10^{6} ton]','FontSize',12);
xlim([startyear endyear]);
set(gca,'FontSize',12);
subplot(2,3,3);
plot(year,rep0,'b','LineWidth',3);
hold on;
plot(year,rep1,'r','LineWidth',3);
plot(year,rep2,'g','LineWidth',3);

text(year(loc_0),rep0(loc_0)*1.1,'Tier-0','Color','b','FontSize',14);
text(year(loc_1),rep1(loc_1)*1.1,'Tier-1','Color','r','FontSize',14);
text(year(loc_2),rep2(loc_2)*1.1,'Tier-2','Color','g','FontSize',14);
ylabel('Reprocessing Throughput by Tier [ton/yr]','FontSize',12);
xlim([startyear endyear]);
set(gca,'FontSize',12);
subplot(2,3,4);
plot(year,coe,'b','LineWidth',2);
hold on;
text(year(20),coe(20)+.25,'COE','Color','b','FontSize',12);
plot(year,fc,'Color',[.4 .2 .6],'LineWidth',2);
text(year(18),fc(18)+.25,'Fuel Cycle','Color',[.4 .2 .6],'FontSize',12);
ylabel('Cost [cents/kWeh]');
% ylim([0 max(coe)*1.1]);
xlim([startyear endyear]);
plot([startyear endyear],[lwrcoe lwrcoe],'-.','Color','Black');
text(2038,3.5,'ALWR Once-Through COE','FontSize',8);
plot([startyear endyear],[lwrfc lwrfc],'-.','Color','Black');
text(2032,0.64,'ALWR Once-Through Fuel Cycle','FontSize',8);
errorbar(2060,lwrcoe,.72,'k');
errorbar(2060,lwrfc,.12,'k');
set(gca,'FontSize',12);
subplot(2,3,5);
plot(year,sf+legacysf,'Color',[.7 0 .7],'LineWidth',3);
text(year(loc_1),sf(loc_1)*1.3,'Total IHM in Fuel','Color',[.7 0 .7],'FontSize',14);
hold on;
plot([min(year) max(year)],[63000 63000],'--','Color','Black');
text(2020,68000,'Yucca Mountain Statutory Limit');
hold on;
plot(year,sfoop+legacysf,'Color',[.4 .7 .9],'LineWidth',1.5);
text(year(loc_0),sfoop(loc_0),'Total SF (Out of Pile)','Color',[.4 .7 .9],'FontSize',12);
plot(year,sflt+legacysf,'Color',[.8 .2 0],'LineWidth',2);
text(year(loc_0),sflt(loc_0),'SF in Long-Term Storage','Color',[.80 .2 .0],'FontSize',12);
if(legacysf > 0.)
    plot([startyear endyear],[legacysf legacysf],'Color',[.2 .6 .1],'LineWidth',1.5);
    text(year(50),legacysf*1.1,'Legacy SF','Color',[.2 .6 .1],'FontSize',12);
end;
for(i=1:n_rx)
    if(max(rx_sf(:,i))>1.)
        plot(year,rx_sf(:,i),'Color',cp(:,i)','LineWidth',1.5);
        text(year(50),rx_sf(50,i)*.95,rx_names(i),'Color',cp(:,i)','FontSize',12);
    end;
end
ylabel('Spent Fuel Inventory [ton IHM]','FontSize',12);
% ylim([0 2000000000]);
xlim([startyear endyear]);
set(gca,'FontSize',12);
subplot(2,3,6);
[AX,H1,H2]=plotyy(year,[pu pult],year,[ma malt]);
hold on;
%set(AX(1),'YLim',[0 1600000]);
%set(AX(2),'YLim',[0 400000]);
%set(AX(1),'YLim',[0 25000]);
%set(AX(2),'YLim',[0 5000]);
%set(AX(1),'YTick',[0 2e5 4e5 6e5 8e5 10e5 12e5 14e5 16e5]);
%set(AX(2),'YTick',[0 5e4 10e4 15e4 20e4 25e4 30e4 35e4 40e4]);
%set(AX(1),'YTick',[0 5e3 10e3 15e3 20e3 25e3]);
%set(AX(2),'YTick',[0 10e2 20e2 30e2 40e2 50e2]);
set(get(AX(1),'Ylabel'),'String','Plutonium Inventory [ton]','Color',[0 .2 .7]);
set(AX(1),'YColor',[0 .2 .7]);
set(AX(2),'YColor',[0 .7 .2]);
set(H1(1),'Color',[0 .2 .7]);
set(H1(2),'Color',[0 .5 1]);
set(H2(1),'Color',[0 .7 .2]);
set(H2(2),'Color',[0 1 .5]);
set(AX(1),'XLim',[startyear endyear]);
set(get(AX(2),'Ylabel'),'String','Minor Actinide Inventory [ton]','Color',[0 .7 .2]);
set(AX(1),'FontSize',12);
set(AX(2),'FontSize',12);
set(AX(2),'XLim',[startyear endyear]);
set(H1(1),'LineWidth',3);
set(H2(1),'LineWidth',3);
set(H1(2),'LineWidth',2);
set(H2(2),'LineWidth',2);
text(year(loc_2),pu(loc_2)*1.1,'Total Pu','Color',[0 .2 .7],'FontSize',14);
text(year(loc_1),ma(loc_1)*max(pu)/max(ma)*0.9,'Total MA','Color',[0 .7 .2],'FontSize',14);
text(year(loc_0),pult(loc_0)*.84,'Pu in Long-Term Storage','Color',[0 .5 1],'FontSize',12);
text(year(loc_0),malt(loc_0)*max(pu)/max(ma)*.76,'MA in Long-Term Storage','Color',[0 1 .5],'FontSize',12);