DIAGS=$(wildcard *.diag)
TARGETS=$(DIAGS:.diag=.svg)

all: $(TARGETS)

%.svg: %.diag
	seqdiag -T svg $<

.PHONY clean:
	rm -f $(TARGETS)
