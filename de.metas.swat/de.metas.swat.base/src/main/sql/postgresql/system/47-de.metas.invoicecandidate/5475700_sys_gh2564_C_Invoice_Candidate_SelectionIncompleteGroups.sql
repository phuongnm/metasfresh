drop view if exists C_Invoice_Candidate_SelectionIncompleteGroups;
create or replace view C_Invoice_Candidate_SelectionIncompleteGroups as
select distinct
	s.AD_PInstance_ID,
	ic.C_Order_ID,
	o.DocumentNo as OrderDocumentNo,
	ic.GroupNo
from T_Selection s
inner join C_Invoice_Candidate ic on (ic.C_Invoice_Candidate_ID=s.T_Selection_ID)
inner join C_Invoice_Candidate ic2 on (ic2.C_Order_ID=ic.C_Order_ID and ic2.GroupNo=ic.GroupNo)
inner join C_Order o on (o.C_Order_ID=ic.C_Order_ID)
where true
and ic.GroupNo > 0
and not exists (select 1 from T_Selection s2 where s2.AD_PInstance_ID=s.AD_PInstance_ID and s2.T_Selection_ID=ic2.C_Invoice_Candidate_ID)
;

-- select * from C_Invoice_Candidate_SelectionIncompleteGroups;

