api-ntrip-java-client
=====================

Neste repositório encontra-se um projecto Android, este projecto é um serviço para acesso a servidores que utilizam o protocolo NTRIP, este projecto teve como base código open source que pode ser encontrado em  lefebure.com. 

Existem três entidades diferentes.

- Um serviço NTRIP que permite a gestão de uma interface Bluetooth e de uma ligação TCP/IP, com o servidor de NTRIP. Os conteúdos RTCM provenientes da estação de referencia são enviados para uma porta série do GPS diferencial que por sua vez está ligado a uma interface Bluetooth. O código encontra-se na biblioteca NTRIPLib (`NTRIPService.java`) .
- Uma classe que permite a interface com o serviço acima descrito de um a forma bastante fácil, encontra-se na biblioteca NTRIPLib (`NTrip.java`).
- Uma terceira entidade é uma aplicação exemplo que permite o programador avaliar a forma de utilizar o a serviço NTRIP, encontra-se em NTRIPCustom.

## Processo de linkagem da biblioteca ##

As instruções apresentadas aplicam-se ao Eclipse 3.7 com ADT plugin versão 18.0.0 ou superior.

No Eclipse, importar o projecto da biblioteca:

> 1. Menu File Import;
> 2. Existing projects into workspace;
> 3. Selecionar o projecto com a biblioteca (NTRIPLib);

Verificar se o projecto é biblioteca:

> 1. Selecionar o projecto no Package Explorer;
> 2. Menu Project Properties
> 3. Selecionar Android
> 4. Verificar que o "Is Library" está selecionado

Criar um novo projecto de android (`Menu New > Android project`) com uma actividade.
Utilizar a biblioteca previamente importada:

> 1. Selecionar o projecto no Package Explorer;
> 2. Menu Project Properties
> 3. Selecionar Android
> 4. Add
> 5. Selecionar a biblioteca e carregar ok
> 6. Carregar ok novamente

##Instrucções de utilização da biblioteca##

Na actividade, criar uma instância da classe de biblioteca:

    NTrip ntrip = new NTrip(activity) {
	    @Override
	    public void UpdateStatus(String fixtype, String info1, String info2) {}

	    @Override
	    public void UpdateLogAppend(String msg) {}

	    @Override
	    public void UpdatePosition(double time, double lat, double lon) 
        {android.util.Log.d("Debug","Posição actualizada "+time+" "+lat+"º "+lon+"º");}

	    @Override
	    public void onServiceConnected() {	}
     };

Definir as opções NTrip:

    ntrip.MACAddress = bluetoothMAC;
    ntrip.MOUNTPOINT = mountpoint;
    ntrip.SERVERIP = serverIP;
    ntrip.SERVERPORT = serverPORT;
    ntrip.SendGGAToServer = true;
    ntrip.NetworkProtocol = "ntripv1";
    ntrip.USERNAME = server_username;
    ntrip.PASSWORD = server_pass;

Arrancar o sistema NTrip:

`ntrip.Connect();`

Fechar o sistema NTrip:

`ntrip.Disconnect();`


##Instrucções de utilização do exemplo##

As instruções apresentadas aplicam-se ao Eclipse 3.7 com ADT plugin versão 18.0.0 ou superior.

No Eclipse, importar o projecto da biblioteca:
> 1. Menu File Import;
> 2. Existing projects into workspace;
> 3. Selecionar o projecto com a biblioteca (NTRIPLib);

No Eclipse, importar o projecto de exemplo:

> 1. Menu File Import;
> 2. Existing projects into workspace;
> 3. Selecionar o projecto com o exemplo (NTRIPCustom);
